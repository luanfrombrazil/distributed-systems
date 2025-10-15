package com.luanlana.chat.client.api;

import com.luanlana.chat.client.model.Message;
import lombok.Setter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/*
* ESSA CLASSE É A RESPONSÁVEL PELAS CONEXÕES INTERMITENTES DO SERVIDOR COM O CLIENTE.
* ALÉM DISSO IMPLEMENTEI AQUI OS MÉTODOS DE DESCONECTAR E CONECTAR O CLIENTE DE TEMPOS EM TEMPOS.
* */
public class StompSessionManager {
    private Random random;

    private static final int MAX_TRIES = 15;
    private final String serverUrl = "ws://localhost:8081/ws-chat";
    private StompSession stompSession;

    private volatile Instant lastActivityTime;
    private volatile Instant lastDisconnectionTime;

    //DEFINI AQUI QUE O CLIENTE PODE SER RECONECTADO APÓS 5SEG DE DESCONEXÃO
    private final ScheduledExecutorService reconectorScheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long RECONECTING_INTERVAL_SECONDS = 5;

    //DEFINI AQUI QUE O CLIENTE SERÁ DESCONECTADO APÓS 5SEG DE INATIVIDADE
    private final ScheduledExecutorService idleCheckerScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> idleCheckTask;
    private static final long IDLE_TIMEOUT_SECONDS = 5;

    private Long lastGroupId;
    private MessageHandler lastMessageHandler;

    @Setter
    private MessageListener listener;
    @Setter
    private ChatApiService apiService;

    public StompSessionManager(){
        //ESSA THREAD NUNCA PARA, ELA VERIFICA SE O CLIENTE ESTÁ A X TEMPO DESCONECTADO PARA TENTAR RECONECTÁ-LO
        startReconectorScheduler();
    }

    //AQUI CONECTA O CLIENTE E DEFINE POR ONDE AS MENSAGENS SERÃO TRATADAS(MESSAGE HANDLER)
    public void connect(Long groupId, MessageHandler messageHandler) throws InterruptedException, ExecutionException {
        if (isConnected()) {
            return;
        }
        this.lastGroupId = groupId;
        this.lastMessageHandler = messageHandler;

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        this.stompSession = stompClient.connectAsync(serverUrl, new StompSessionHandlerAdapter() {
        }).get();

        listener.statusOn();
        startIdleChecker();
        subscribeToGroupTopic(groupId, messageHandler);
        lastActivityTime = Instant.now();

    }

    private void subscribeToGroupTopic(Long groupId, MessageHandler messageHandler) {
        String topic = "/groups/" + groupId;
        stompSession.subscribe(topic, messageHandler);
    }

    public boolean isConnected() {
        return this.stompSession != null && this.stompSession.isConnected();
    }

    public void resetTimer() {
        this.lastActivityTime = Instant.now();
    }

    public void disconnect() {
        if (this.idleCheckTask != null && !this.idleCheckTask.isDone()) {
            this.idleCheckTask.cancel(true);
        }
        if (isConnected()) {
            lastDisconnectionTime = Instant.now();
            apiService.setLastSyncTimestamp(Instant.now());
            listener.statusOff();
            stompSession.disconnect();
        }
        this.stompSession = null;
    }

    //AQUI É O RESPONSÁVEL POR DESCONECTAR CLIENTES INATIVOS
    private void startIdleChecker() {
        this.idleCheckTask = idleCheckerScheduler.scheduleAtFixedRate(() -> {
            if (lastActivityTime != null) {
                long idleSeconds = Duration.between(lastActivityTime, Instant.now()).getSeconds();
                System.out.print("INATIVO: " + idleSeconds + "s.");
                if (idleSeconds > IDLE_TIMEOUT_SECONDS) {
                    disconnect();
                }
            }
        }, 4, 1, TimeUnit.SECONDS);
    }

    //AQUI FAZ AS VERIFICAÇÕES E RECONECTA O CLIENTE
    private void startReconectorScheduler() {
        reconectorScheduler.scheduleAtFixedRate(() -> {
            try {
                if (!isConnected() && lastDisconnectionTime != null && lastGroupId != null) {
                    long secondsSinceDisconnect = Duration.between(lastDisconnectionTime, Instant.now()).getSeconds();
                    if (secondsSinceDisconnect >= RECONECTING_INTERVAL_SECONDS) {
//                        System.out.println("RECONECTING: Tempo de espera atingido. Tentando buscar novas mensagens...");

                        performMessageCheck();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("ERRO AO BUSCAR MENSAGENS" + e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /*
    * AQUI É FEITO UMA BUSCA POR MENSAGENS NOVAS APÓS O CLIENTE TER FICADO UM TEMPO SEM CONEXAO
    * */
    private void performMessageCheck() throws InterruptedException {
        long baseDelayMs = 500;
        for (int attempt = 1; attempt <= MAX_TRIES; attempt++) {
            try {
//                System.out.println("RECONECTING: Reconectando para buscar novas mensagens...");
                connect(this.lastGroupId, this.lastMessageHandler);
                List<Message> history = apiService.fetchMessages(this.lastGroupId, apiService.getLastSyncTimestamp());
                listener.onHistoryReceived(history);
                return;
            } catch (Exception e) {
//                System.err.println("RECONECTING: Falha ao tentar reconectar e buscar mensagens: " + e.getMessage());
                if (attempt == MAX_TRIES) {
                    throw new RuntimeException("VERIFIQUE SE O SERVIDOR ESTÁ ONLINE", e);
                }
                long backoffDelay = baseDelayMs * (long) Math.pow(2, attempt - 1);
                long jitter = random.nextInt(1000);
                long delay = backoffDelay + jitter;
                Thread.sleep(delay);
            }
        }
        throw new RuntimeException("FALHA AO BUSCAR HISTÓRICO DE MENSAGENS. ");
    }
}