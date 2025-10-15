package com.luanlana.chat.client.view;

import com.luanlana.chat.client.api.ChatApiService;
import com.luanlana.chat.client.api.MessageHandler;
import com.luanlana.chat.client.api.MessageListener;
import com.luanlana.chat.client.api.StompSessionManager;
import com.luanlana.chat.client.model.Group;
import com.luanlana.chat.client.model.Message;
import com.luanlana.chat.client.dto.MessageRequest;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class ChatWindowController implements MessageListener {

    private static final String CONNECTION_PANEL_KEY = "CONNECTION_PANEL";
    private static final String CHAT_PANEL_KEY = "CHAT_PANEL";
    private static final String ADD_GROUP_PANEL_KEY = "ADD_GROUP_PANEL";

    private final StompSessionManager stompManager;
    private final MessageHandler messageHandler;
    private final ChatApiService chatApiService;

    private final JFrame frame;
    private final CardLayout cardLayout;
    private final JPanel mainCardPanel;

    private final Map<String, Color> userColors = new HashMap<>();
    private final Random random = new Random();
    private JTextField usernameField;
    private JComboBox<String> groupComboBox;
    private JTextPane chatArea;
    private JTextField messageField;
    private String currentPanelKey;
    private JTextField addGroupUsernameField;
    private JTextField newGroupNameField;
    private JButton statusButton;

    private Map<String, Long> groupNamesId = new HashMap<>();

    private String nickname;
    private String group;

    String lastNicknamePrinted;


    public ChatWindowController(StompSessionManager stompManager, ChatApiService chatApiService, MessageHandler messageHandler) {
        this.stompManager = stompManager;
        this.chatApiService = chatApiService;
        this.messageHandler = messageHandler;

        //UM NOME ALEATÓRIO SÓ PARA NÃO TER CONFLITO
        this.lastNicknamePrinted = "ak,md9na7bs8dyta7nsdy8b";

        frame = new JFrame("CONECTAR AO CHAT");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setSize(600, 450);

        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);

        JPanel connectionPanel = connectionPanel();
        JPanel chatPanel = chatPanel();
        JPanel addGroupPanel = addGroupPanel();

        mainCardPanel.add(connectionPanel, CONNECTION_PANEL_KEY);
        mainCardPanel.add(chatPanel, CHAT_PANEL_KEY);
        mainCardPanel.add(addGroupPanel, ADD_GROUP_PANEL_KEY);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClose();
            }
        });
        frame.add(mainCardPanel);
        this.currentPanelKey = CONNECTION_PANEL_KEY;
        frame.setVisible(true);
    }

    /*
    TELA PRINCIPAL/INICILA, AQUI O USUÁRIO ESCOLHE UM GRUPO DA LISTA(PODE ATUALIZAR CASO FALTE UM JA CRIADO),
    O USUÁRIO PODE ESCOLHER SEU NICKNAME QUE SERÁ USADO NO CHAT. O BOTÃO COM SETINHA(REFRESH) ATUALIZA A LISTA, O BOTÃO +
    PERMITE O USUÁRIO CRIAR UM NOVO GRUPO (É CONECTADO AUTOMATICAMENTE)

    O BOTÃO DE FECHAR FECHA O PROGRAMA SOMENTE NESSA TELA PRINCIPAL, NAS DEMAIS TELAS ELE SERVE COMO BOTÃO DE VOLTAR/DESCONECTAR

     */
    private JPanel connectionPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);


        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("USUÁRIO:"), gbc);

        usernameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(usernameField, gbc);


        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("GRUPO:"), gbc);

        groupComboBox = new JComboBox<>();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(groupComboBox, gbc);

        JPanel smallButtonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton refreshButton = new JButton("⟳");
        refreshButton.setMargin(new Insets(2, 5, 2, 5));
        JButton addButton = new JButton("+");
        addButton.setMargin(new Insets(2, 5, 2, 5));
        smallButtonPanel.add(refreshButton);
        smallButtonPanel.add(addButton);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(smallButtonPanel, gbc);

        JButton connectButton = new JButton("CONECTAR");
        Dimension buttonSize = refreshButton.getPreferredSize();
        int connectButtonWidth = (int) (buttonSize.getWidth() * 4) + 20;
        connectButton.setPreferredSize(new Dimension(connectButtonWidth, (int) buttonSize.getHeight()));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(connectButton, gbc);

        connectButton.addActionListener(e -> {
            //CONNECT BUTTON
            String nickname = usernameField.getText();
            if (nickname == null || nickname.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "USUÁRIO NÃO PODE SER VAZIO", "ERRO DE USERNAME", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String group = (String) groupComboBox.getSelectedItem();
            Long groupId = groupNamesId.get(group);
            if (groupId == null) {
                JOptionPane.showMessageDialog(frame, "GRUPO INVÁLIDO. POR FAVOR, ATUALIZE A LISTA.", "ERRO DE GRUPO", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ArrayList<JButton> disable = new ArrayList<>();
            disable.add(addButton);
            disable.add(connectButton);
            disable.add(refreshButton);
            try {
                connectToChat(groupId, nickname, group, disable);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            frame,
                            "FALHA AO CONECTAR AO CHAT:\n" + ex.getMessage(),
                            "ERRO DE INTERRUPÇÃO",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
            }
        });

        /*
        * LI QUE É IMPORTANTE UTILIZAR THREADS PARA EVITAR PROBLEMAS COM O JAVAX SWING E NÃO TER FUNCIONALIDADES
        * INTERROMPIDAS/TRAVADAS, ENTÃO EM LUGARES QUE FAZEM CONEXÃO FIXA COM O SERVIDOR, EU OPTEI POR INVOCAR UMA
        * NOVA THREAD, E EM LOCAIS QUE FAZEM ALTERAÇÕES NA INTERFACE TAMBÉM OPTEI POR THREADS.
        * */

        addButton.addActionListener(e -> {
            String currentUsername = usernameField.getText();
            addGroupUsernameField.setText(currentUsername);
            this.currentPanelKey = ADD_GROUP_PANEL_KEY;
            cardLayout.show(mainCardPanel, ADD_GROUP_PANEL_KEY);
        });

        refreshButton.addActionListener(e -> {
            refreshButton.setEnabled(false);
            connectButton.setEnabled(false);
            new Thread(() -> {
                try {
                    final ArrayList<Group> groupsFromServer = chatApiService.fetchGroups();
                    SwingUtilities.invokeLater(() -> {
                        groupComboBox.removeAllItems();
                        groupNamesId.clear();

                        for (Group group : groupsFromServer) {
                            groupNamesId.put(group.getRoomName(), group.getId());
                            groupComboBox.addItem(group.getRoomName());
                        }
                    });

                } catch (Exception ex) {

                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                frame,
                                "FALHA AO BUSCAR OS GRUPOS:\n" + ex.getMessage(),
                                "ERRO DE CONEXÃO",
                                JOptionPane.ERROR_MESSAGE
                        );
                    });
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        refreshButton.setEnabled(true);
                        connectButton.setEnabled(true);
                    });
                }
            }).start();
        });

        JPanel centeringPanel = new JPanel(new GridBagLayout());
        centeringPanel.add(formPanel);
        new Thread(() -> {
            try {
                final ArrayList<Group> groupsFromServer = chatApiService.fetchGroups();

                SwingUtilities.invokeLater(() -> {
                    groupComboBox.removeAllItems();
                    groupNamesId.clear();

                    for (Group group : groupsFromServer) {
                        groupNamesId.put(group.getRoomName(), group.getId());
                        groupComboBox.addItem(group.getRoomName());
                    }
                });

            } catch (Exception ex) {

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            frame,
                            "FALHA AO BUSCAR GRUPOS NO SERVIDOR\n VERIFIQUE SE O SERVIDOR ESTÁ ONLINE\nERRO :" + ex.getMessage(),
                            "ERRO DE CONEXAP",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    refreshButton.setEnabled(true);
                    connectButton.setEnabled(true);
                });
            }
        }).start();
        return centeringPanel;
    }


    /*
     * AQUI É O PAINEL DE CRIAÇÃO DE GRUPO, OPTEI POR FAZER A CONEXÃO DIRETO COM O SERVIDOR E PULAR A ETAPA DE SELEÇÃO NA
     * TELA PRINCIPAL.
     * */

    private JPanel addGroupPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        formPanel.add(new JLabel("USUÁRIO:"), gbc);
        addGroupUsernameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(addGroupUsernameField, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;

        formPanel.add(new JLabel("GRUPO:"), gbc);
        newGroupNameField = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(newGroupNameField, gbc);

        JButton saveButton = new JButton("SALVAR");
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actionPanel.add(saveButton);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(actionPanel, gbc);


        saveButton.addActionListener(e -> {
            String group = newGroupNameField.getText();
            if (group == null || group.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "NOME DO GRUPO NAO PODE ESTAR VAZIO", "ERRO", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String nickname = addGroupUsernameField.getText();
            if (nickname == null || nickname.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "O NOME DE USUÁRIO NAO PODE ESTAR VAZIO", "ERRO", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (group != null) {
                new Thread(() -> {
                    try {
                        Group novo_grupo = chatApiService.newGroup(group);
                        groupNamesId.put(novo_grupo.getRoomName(), novo_grupo.getId());
                        Long groupId = groupNamesId.get(group);
                        ArrayList<JButton> desabilitar = new ArrayList<>();
                        desabilitar.add(saveButton);
                        connectToChat(groupId, nickname, group, desabilitar);
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                    frame,
                                    "FALHA AO CRIAR UM NOVO GRUPO\n" + ex.getMessage(),
                                    "ERRO DE CONEXAO",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        });
                    }
                }).start();
            } else {
                JOptionPane.showMessageDialog(frame, "O NOME DO GRUPO NAO PODE ESTAR VAZIO", "EROO", JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel centeringPanel = new JPanel(new GridBagLayout());
        centeringPanel.add(formPanel);
        return centeringPanel;
    }

    /*
    * AQUI É A TELA DE CHAT, UM CAMPO DE TEXTO ONDE AS MENSAGENS APARECE, UM BOTÃO DE ENVIO
    * UM CAMPO PARA DIGITAR A MENSAGEM A SER ENVIADA E UM "BOTÃO" QUE INDICA O STATUS DO USUÁRIO.
    *
    * VARIAS MUDANÇAS OCORRERAM CONFORME OS REQUISITOS FORAM SENDO ESCLARECIDOS EM SALA, UM DELES FOI O ATUAL BOTÃO DE STATUS
    * QUE ERA UM BOTÃO DE BUSCA DE MENSAGENS ATUAIS / RECONEXÃO DO CLIENTE APÓS INATIVIDADE.
    * PARA NÃO QUEBRAR A ESTÉTICA DA JANELINHA, ACABEI ADAPTANDO PARA ELE MUDAR DE COR (ESSA ESCOLHA ACABOU AFETANDO
    *  ALGUMAS IMPLEMENTAÇÕES E ESCOLHAS, COMO O "MESSAGE LISTENER" PASSAR A TER MÉTODOS DE MUDAR COR DE BOTÃO. O QUE
    * NÃO FAZ SENTIDO SENDO QUE A FUNÇÃO DELE É AS MUDANÇAS AO "OUVIR MENSAGEM".
    *
    * GRANDE PARTE DO PROJETO ESTÁ EM TORNO DAS FUNCIONALIDADES ABAIXO
    *
    * */
    private JPanel chatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFocusable(false);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        messageField = new JTextField(40);
        JButton sendButton = new JButton("ENVIAR");
        inputPanel.add(messageField, BorderLayout.WEST);
        statusButton = new JButton(" ");
        inputPanel.add(statusButton, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        Action sendMessageAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String message = messageField.getText();
                if (message == null || message.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "MENSAGEM NAO PODE ESTAR VAZIA", "ERRO", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                MessageRequest request = new MessageRequest(UUID.randomUUID().toString(), message, nickname, Instant.now());
                Long groupId = groupNamesId.get(group);
                messageField.setText("");
                new Thread(() -> {
                    try {
                        //AQUI ESTÁ A PRINCIPAL FUNÇÃO:
                        chatApiService.sendMessage(groupId, request);
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                    frame,
                                    "FALAH AO ENVIAR MENSAGEM AO SERVIDOR:\n" + ex.getMessage(),
                                    "ERRO DE CONEXAO",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        });
                    }
                }).start();
            }
        };

        sendButton.addActionListener(sendMessageAction);
        messageField.addActionListener(sendMessageAction);
        return chatPanel;
    }


    /*
    * AQUI FOI DEFINIDO QUE O BOTÃO DE FECHAR SERIA EQUIVALENTE A VOLTAR E FINALIZAR APENAS NA TELA PRINCIPAL
    * */
    private void handleWindowClose() {
        if (CHAT_PANEL_KEY.equals(this.currentPanelKey) || ADD_GROUP_PANEL_KEY.equals(this.currentPanelKey)) {
            stompManager.disconnect();
            chatArea.setText("");
            cardLayout.show(mainCardPanel, CONNECTION_PANEL_KEY);
            this.currentPanelKey = CONNECTION_PANEL_KEY;
            frame.setTitle("CONECTAR AO CHAT");
        } else {
            System.exit(0);
        }
    }

    /*
    * APENAS ESCOLHA ESTETICA DE CADA USERNAME TER UMA COR NO CHAT
    * */
    private Color getColorForUser(String username) {
        return userColors.computeIfAbsent(username, u -> {
            float hue = random.nextFloat();
            float saturation = 0.5f + random.nextFloat() * 0.5f;
            float brightness = 0.7f + random.nextFloat() * 0.3f;
            return Color.getHSBColor(hue, saturation, brightness);
        });
    }

    /*
    * ADICIONA TEXTO NA TELA DE CHAT, CASO A MENSAGEM A SER IMPRESSA SEJA A ENVIADA PELO CLIENTE ATUAL OU TENHA O MESMO NOME
    * ELA É ALINHADA A DIREITA E O USERNAME É SUBSTITUIDO POR "VOCE"
    * */
    private void appendColoredText(String username, String message) {
        StyledDocument doc = chatArea.getStyledDocument();
        int startOffset = doc.getLength();

        SimpleAttributeSet userStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(userStyle, getColorForUser(username));
        StyleConstants.setBold(userStyle, true);

        SimpleAttributeSet messageStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(messageStyle, Color.WHITE);
        StyleConstants.setBold(messageStyle, false);

        try {
            SimpleAttributeSet paragraphStyle = new SimpleAttributeSet();
            if (username.equals(this.nickname)) {
                if (!username.equals(lastNicknamePrinted))
                    doc.insertString(doc.getLength(), "VOCÊ: ", userStyle);
                StyleConstants.setAlignment(paragraphStyle, StyleConstants.ALIGN_RIGHT);

            } else {
                if (!username.equals(lastNicknamePrinted))
                    doc.insertString(doc.getLength(), username + ": ", userStyle);
                StyleConstants.setAlignment(paragraphStyle, StyleConstants.ALIGN_LEFT);
            }
            doc.insertString(doc.getLength(), message + "\n", messageStyle);
            doc.setParagraphAttributes(startOffset, doc.getLength() - startOffset, paragraphStyle, false);
            this.lastNicknamePrinted = username;
        } catch (Exception e) {
            e.printStackTrace();
        }
        chatArea.setCaretPosition(doc.getLength());
    }

    /*
     * AQUI O USUÁRIO É CONECTADO AO CHAT, UTILIZANDO O GROUPID, NICKNAME, NOME DO GRUPO E QUAIS BOTÕES DEVEM SER
     * DESABILITADOS DAS INTERFACES DAS QUAIS A REQUISIÇÃO FOI FEITA, ATÉ QUE A CONEXÃO SEJA ESTABELECIDA OU FALHE.
     */


    private void connectToChat(Long groupId, String nickname, String groupName, List<JButton> buttons) {
        buttons.forEach(b -> b.setEnabled(false));
        new Thread(() -> {
            try {
                stompManager.connect(groupId, messageHandler);
                List<Message> mensagens = chatApiService.fetchMessages(groupId, Instant.now().minus(10, ChronoUnit.MINUTES));
                onHistoryReceived(mensagens);
                SwingUtilities.invokeLater(() -> {
                    frame.setTitle("CHAT - " + nickname + " @ " + groupName);
                    cardLayout.show(mainCardPanel, CHAT_PANEL_KEY);
                    this.currentPanelKey = CHAT_PANEL_KEY;
                    this.nickname = nickname;
                    this.group = groupName;
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "FALHA AO CONECTAR AO CHAT: " + ex.getMessage(),
                            "ERRO DE CONEXAO", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    buttons.forEach(b -> b.setEnabled(true));
                });
            }
        }).start();
    }

    //CASO RECEBA MENSAGEM
    @Override
    public void onMessageReceived(Message message) {
        SwingUtilities.invokeLater(() -> {
            appendColoredText(message.getNickname(), message.getMessage());
        });
    }

    //APÓS RECEBER VARIAS MENSAGENS
    @Override
    public void onHistoryReceived(List<Message> messageHistory) {
        SwingUtilities.invokeLater(() -> {
            for (Message message : messageHistory) {
                appendColoredText(message.getNickname(), message.getMessage());
            }
        });
    }

    //MUDA A COR DO BOTÃO DE STATUS.
    @Override
    public void statusOn() {
        SwingUtilities.invokeLater(() -> {
            this.statusButton.setBackground(Color.GREEN);
        });
    }

    @Override
    public void statusOff() {
        SwingUtilities.invokeLater(() -> {
            this.statusButton.setBackground(Color.RED);
        });
    }
}