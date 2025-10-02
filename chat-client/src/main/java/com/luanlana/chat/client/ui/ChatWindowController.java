package com.luanlana.chat.client.ui;

import com.luanlana.chat.client.api.ChatApiService;
import com.luanlana.chat.client.api.MessageHandler;
import com.luanlana.chat.client.api.MessageListener;
import com.luanlana.chat.client.api.StompSessionManager;
import com.luanlana.chat.client.model.Group;
import com.luanlana.chat.client.model.Message;
import com.luanlana.chat.client.model.MessageRequest;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
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

    private Map<String, Long> groupNamesId = new HashMap<>();

    private String nickname;
    private String group;

    String lastNicknamePrinted;

    public ChatWindowController(StompSessionManager stompManager, ChatApiService chatApiService, MessageHandler messageHandler) {
        this.stompManager = stompManager;
        this.stompManager.setWindowController(this);
        this.chatApiService = chatApiService;
        this.messageHandler = messageHandler;

        this.lastNicknamePrinted = "ak,md9na7bs8dyta7nsdy8b";

        frame = new JFrame("Conectar ao Chat");
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

    private JPanel connectionPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);


        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Usuário:"), gbc);

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
        formPanel.add(new JLabel("Grupo:"), gbc);

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

        JButton connectButton = new JButton("Conectar");
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
                JOptionPane.showMessageDialog(frame, "O nome de usuário não pode estar vazio.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String group = (String) groupComboBox.getSelectedItem();
            Long groupId = groupNamesId.get(group);
            if (groupId == null) {
                JOptionPane.showMessageDialog(frame, "Grupo inválido. Por favor, atualize a lista.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ArrayList<JButton> desabilitar = new ArrayList<>();
            desabilitar.add(addButton);
            desabilitar.add(connectButton);
            desabilitar.add(refreshButton);

            connectToChat(groupId, nickname, group, desabilitar);
        });

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
                                "Falha ao buscar grupos do servidor:\n" + ex.getMessage(),
                                "Erro de Conexão",
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
                            "Falha ao buscar grupos do servidor:\n" + ex.getMessage(),
                            "Erro de Conexão",
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

    private JPanel chatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));


        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFocusable(false);


        JScrollPane scrollPane = new JScrollPane(chatArea);
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        messageField = new JTextField();
        JButton sendButton = new JButton("Enviar");
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        Action sendMessageAction = new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String message = messageField.getText();
                MessageRequest request = new MessageRequest(UUID.randomUUID().toString(), message, nickname, Instant.now());
                Long groupId = groupNamesId.get(group);
                messageField.setText("");
                new Thread(() -> {
                    try {
                        chatApiService.sendMessage(groupId, request);
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                    frame,
                                    "Falha enviar mensagem para o servidor:\n" + ex.getMessage(),
                                    "Erro de Conexão",
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

    private JPanel addGroupPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        formPanel.add(new JLabel("Usuário:"), gbc);
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

        formPanel.add(new JLabel("Grupo:"), gbc);
        newGroupNameField = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(newGroupNameField, gbc);

        JButton saveButton = new JButton("Salvar");
        JButton cancelButton = new JButton("Cancelar");
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actionPanel.add(saveButton);
        actionPanel.add(cancelButton);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(actionPanel, gbc);


        cancelButton.addActionListener(e -> {
            cardLayout.show(mainCardPanel, CONNECTION_PANEL_KEY);
        });

        saveButton.addActionListener(e -> {
            String group = newGroupNameField.getText();
            if (group == null || group.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "O nome do grupo não pode estar vazio.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String nickname = addGroupUsernameField.getText();
            if (nickname == null || nickname.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "O nome de usuário não pode estar vazio.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (group != null && !group.trim().isEmpty()) {
                new Thread(() -> {
                    try {
                        Group novo_grupo = chatApiService.newGroup(group);
                        groupNamesId.put(novo_grupo.getRoomName(), novo_grupo.getId());
                        Long groupId = groupNamesId.get(group);
                        ArrayList<JButton> desabilitar = new ArrayList<>();
                        desabilitar.add(saveButton);
                        desabilitar.add(cancelButton);
                        connectToChat(groupId, nickname, group, desabilitar);
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                    frame,
                                    "Falha ao criar um novo grupo no servidor:\n" + ex.getMessage(),
                                    "Erro de Conexão",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        });
                    }
                }).start();
            } else {
                JOptionPane.showMessageDialog(frame, "O nome do grupo não pode estar vazio.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel centeringPanel = new JPanel(new GridBagLayout());
        centeringPanel.add(formPanel);
        return centeringPanel;
    }


    private void handleWindowClose() {
        if (CHAT_PANEL_KEY.equals(this.currentPanelKey) || ADD_GROUP_PANEL_KEY.equals(this.currentPanelKey)) {
            stompManager.disconnect();
            chatArea.setText("");
            cardLayout.show(mainCardPanel, CONNECTION_PANEL_KEY);
            this.currentPanelKey = CONNECTION_PANEL_KEY;
            frame.setTitle("Conectar ao Chat");
        } else {
            System.exit(0);
        }
    }

    //COLORIR MENSAGENS
    private Color getColorForUser(String username) {
        return userColors.computeIfAbsent(username, u -> {
            float hue = random.nextFloat();
            float saturation = 0.5f + random.nextFloat() * 0.5f;
            float brightness = 0.7f + random.nextFloat() * 0.3f;
            return Color.getHSBColor(hue, saturation, brightness);
        });
    }

    //MENSAGENS COLORIDINHAS DO TEXTAREA
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
                    doc.insertString(doc.getLength(), "Você: ", userStyle);
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

    private void connectToChat(Long groupId, String nickname, String groupName, List<JButton> buttons) {
        System.out.println("Conectando o usuário " + nickname + " ao grupo:" + groupId + " - " + groupName);
        buttons.forEach(b -> b.setEnabled(false));
        new Thread(() -> {
            try {

                stompManager.connect(groupId, messageHandler);

                List<Message> mensagens = chatApiService.fetchMessages(groupId);
                onHistoryReceived(mensagens);

                SwingUtilities.invokeLater(() -> {
                    frame.setTitle("Chat - " + nickname + " @ " + groupName);
                    cardLayout.show(mainCardPanel, CHAT_PANEL_KEY);
                    this.currentPanelKey = CHAT_PANEL_KEY;
                    this.nickname = nickname;
                    this.group = groupName;
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "Falha ao conectar ao chat: " + ex.getMessage(), "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    buttons.forEach(b -> b.setEnabled(true));
                });
            }
        }).start();
    }

    @Override
    public void onMessageReceived(Message message) {
        SwingUtilities.invokeLater(() -> {
            appendColoredText(message.getNickname(), message.getMessage());
        });
    }

    public void onHistoryReceived(List<Message> messageHistory) {
        SwingUtilities.invokeLater(() -> {
            chatArea.setText("");
            for (Message message : messageHistory) {
                appendColoredText(message.getNickname(), message.getMessage());
            }
        });
    }
}