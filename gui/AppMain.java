package gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static gui.Obstacle.*;

/**
 * 主程序类
 * classe de programme principale
 */
public class AppMain extends Application {

    private static final double WIDTH = 700;
    private static final double HEIGHT = 700;
    private static final double PADDING = 10;
    private static final int SIZE = 16;
    private static final double SIDE = (WIDTH - 2 * PADDING) / (SIZE * 1.0);

    /**
     * 每个玩家回合可操作的时间上限
     * Le temps maximum que le tour de chaque joueur peut opérer
     *  - 一旦超时直接结束该用户回合
     *  Une fois le délai d'attente terminé, le tour de l'utilisateur est directement terminé
     */
    private static final int STEP_TIME = 60;

    /**
     * 用于记录当前玩家回合剩余的时间
     * Utilisé pour enregistrer le temps restant du tour du joueur actuel
     */
    private AtomicInteger currentStepTime = new AtomicInteger(0);
    /**
     * 用于记录当前操作的玩家下标
     * L'indice du joueur utilisé pour enregistrer l'action en cours
     */
    private AtomicInteger currentPlayerIndex = new AtomicInteger(-1);
    /**
     * 用于记录当前被操作的棋子下标
     * Utilisé pour enregistrer l'indice de la pièce d'échecs en cours d'utilisation
     */
    private AtomicInteger currentChessIndex = new AtomicInteger(0);
    /**
     * 用于定时WTimer
     * Pour chronométrer WTimer
     */
    private WTimer moveTimer;
    /**
     * 玩家列表
     * liste des joueurs
     */
    private List<Player> playerList;
    /**
     * 步数输入按钮
     * bouton pour saisir les étapes
     */
    private Button stepInputButton;
    /**
     * 根布局(流布局)
     * disposition racine (disposition de flux)
     */
    private FlowPane root;
    /**
     * 右边的面板根布局(垂直布局)
     * Disposition racine du panneau droit (disposition verticale)
     */
    private VBox rightRoot;

    /**
     * 棋子的图片(提前进行加载，方便复用)
     * La photo de la pièce d'échecs (chargée à l'avance, facile à réutiliser)
     */
    private Image redChessImage = new Image(getClass().getResourceAsStream("/gui/p1.jpg"), SIDE, SIDE, true, true);
    private Image greenChessImage = new Image(getClass().getResourceAsStream("/gui/p2.jpg"), SIDE, SIDE, true, true);
    private Image yellowChessImage = new Image(getClass().getResourceAsStream("/gui/p3.jpg"), SIDE, SIDE, true, true);
    private Image blueChessImage = new Image(getClass().getResourceAsStream("/gui/p4.jpg"), SIDE, SIDE, true, true);

    /**
     * 用于显示当前游戏状态的标签
     * Une étiquette pour afficher l'état actuel du jeu
     */
    private Label infoLabel = new Label("Waiting game start!");
    /**
     * 用于显示当前剩余时间的标签
     * Une étiquette pour indiquer le temps restant actuel
     */
    private Label timeInfoLabel = new Label(String.valueOf(STEP_TIME));
    /**
     * 用于显示当前操作的棋子序号的标签
     * Étiquette utilisée pour afficher le numéro de la pièce en cours d'exploitation
     */
    private Label chessOrderLabel = new Label();

    /**
     * 棋子列表
     * Liste des pièces
     */
    private List<Chess> chessList;

    /**
     * 四个棋子的ImageView对象
     * Objets ImageView pour quatre robots
     */
    private ImageView redChess;
    private ImageView greenChess;
    private ImageView yellowChess;
    private ImageView blueChess;


    /**
     * 障碍物Map集合
     * Obstacle (c'est-à-dire mur) Collection de cartes
     */
    private Map<String, Obstacle> obstacleMap = new HashMap<>();
    /**
     * 非出生点Set集合
     * Collection d'ensembles non liés au point de naissance
     */
    private Set<String> nonBirthPointSet = new HashSet<>();

    /**
     * Start方法，初始化游戏并显示gui界面
     * start method. Initialiser le jeu et afficher l'interface graphique
     * @param stage 舞台scène
     * @throws Exception    异常erreur
     */
    @Override
    public void start(Stage stage) throws Exception {
        root = new FlowPane();                  //整体是一个流布局，就是横向布局L'ensemble est une mise en page fluide, c'est-à-dire une mise en page horizontale
        rightRoot = new VBox(50);       //垂直布局disposition verticale


        initGameGridPane();         //初始化布局Initialiser la mise en page

        executeSecondScheduledService();        //线程，保证倒计时的时候可以操作键盘。因为倒计时是一个线程，操作键盘是一个线程。他们两个需要同时运行
        //Fil pour s'assurer que le clavier peut être utilisé pendant le compte à rebours. Parce que le compte à rebours est un fil, l'utilisation du clavier est un fil. Les deux doivent courir en même temps

        initInfoLabel();


        initPlayerNumberPane();         //初始化玩家initialiser les joueurs

        initChessOrderPane();           //初始化棋子顺序Initialiser l'ordre des pions

        initPlayerOrderPane();

        initPlayerStepInputPane();      //初始化玩家步数的输入框Initialiser la zone de saisie des pas du joueur


        root.getChildren().add(rightRoot);
        Scene scene = new Scene(root, 1100, 700);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * 初始化定时器(这里开辟了新的线程)
     * Initialiser le minuteur (un nouveau fil est ouvert ici)
     *  - 每秒执行一次WTimer对象的update()方法
     *  Exécute la méthode update() de l'objet WTimer toutes les secondes
     */
    private void executeSecondScheduledService() {
        moveTimer = WTimer.createWTimer(0, new WTimer.OnTimerListener() {
            @Override
            public void onTimerRunning(WTimer mTimer) {
                /*
                    注意要使用 Platform.runLater 来执行gui操作！
                    Remarque à utiliser Platform.runLater pour effectuer des opérations d'interface graphique !
                    不可以由其他线程直接操作gui
                    L'interface graphique ne peut pas être directement manipulée par d'autres threads
                        这个方法会将要执行的内容 放入一个消息队列
                        Cette méthode placera le contenu à exécuter dans une file d'attente de messages
                        当主线程有空时从消息队列中取出执行
                        Prendre l'exécution à partir de la file d'attente des messages lorsque le thread principal est libre
                 */
                Platform.runLater(new Runnable() {
                    /**
                     * 剩余时间-1
                     * Temps restant -1
                     *  - 如果时间用完结束该玩家本回合
                     *  Si le temps est écoulé, terminez le tour du joueur
                     *  - 如果时间还剩余则将剩余时间刷新到标签上
                     *  S'il reste du temps, actualisez le temps restant sur l'étiquette
                     */
                    @Override
                    public void run() {
                        int time = currentStepTime.getAndDecrement();
                        if (time <= 0) {
                            // step over
                            nextPlayerStep();
                        } else {
                            timeInfoLabel.setText(String.valueOf(time));
                        }
                    }
                });
            }
        });

        ScheduledService<Void> scheduledService = new ScheduledService<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        moveTimer.update();
//                        System.out.println("---");
//                        System.out.println("playerIndex:" + currentPlayerIndex);
//                        System.out.println("chessIndex:" + currentChessIndex);
                        return null;
                    }
                };
            }

        };
        // Set up a thread pool , 'restart' will try to reuse threads
        scheduledService.setExecutor(Executors.newFixedThreadPool(1));
        // Delay 2s to start
        scheduledService.setDelay(Duration.millis(2000));
        // Execute at an interval of 1 second
        scheduledService.setPeriod(Duration.millis(1000));
        scheduledService.start();
//        testScheduledService.cancel();
//        testScheduledService.restart();
    }

    /**
     * 初始化 游戏状态标签 和 定时器标签
     * initialisation
     * étiquette d'état du jeu
     * et
     * Étiquette de la minuterie
     *  这里使用到 StackPane 将标签居中
     *  Ici, StackPane est utilisé pour centrer l'étiquette
     */
    private void initInfoLabel() {
        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(infoLabel);
        rightRoot.getChildren().add(stackPane);

        StackPane stackPane2 = new StackPane();
        stackPane2.getChildren().add(timeInfoLabel);
        rightRoot.getChildren().add(stackPane2);
    }

    /**
     * 初始化 键盘事件
     * Initialiser les événements du clavier
     *  - 首先判断是否为可操作阶段，不是则过滤掉监听的键盘事件
     *  Déterminez d'abord s'il s'agit d'une étape opérationnelle, sinon, filtrez les événements de clavier surveillés
     *  - 仅监听 上下左右 四个按键，控制当前棋子进行单方向移动，直至撞墙或棋子
     *  Surveillez uniquement les quatre boutons haut, bas, gauche et droite, et contrôlez la pièce actuelle pour
     *  qu'elle se déplace dans une direction jusqu'à ce qu'elle touche le mur ou la pièce
     */
    private void initKeyBoardEvent() {
        root.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            String key = event.getCode().getName();     //当前按下按钮的名字，ex：up， down
            int chessIndex = currentChessIndex.get();
            if (chessIndex < 0 || chessIndex >= chessList.size()) {
                System.err.println("chessIndex < 0 || chessIndex >= chessList.size()");
                return;
            }
            int playerIndex = currentPlayerIndex.get();
            if (playerIndex < 0 || playerIndex >= playerList.size()) {
                System.err.println("playerIndex < 0 || playerIndex >= playerList.size()");
                return;
            }

            ImageView imageView = chessList.get(chessIndex).getImageView();
            switch (key) {
                case "Up":
                    chessMoveUpStraightly(imageView);
                    break;
                case "Down":
                    chessMoveDownStraightly(imageView);
                    break;
                case "Left":
                    chessMoveLeftStraightly(imageView);
                    break;
                case "Right":
                    chessMoveRightStraightly(imageView);
                    break;
                default:
                    break;
            }
//            System.out.println(key);
        });
    }

    /**
     * 控制当前棋子一直向上移动，直至撞到墙或棋子，这一步操作结束
     * Contrôlez la pièce d'échecs actuelle pour qu'elle se déplace vers le haut
     * jusqu'à ce qu'il heurte un mur ou une pièce d'échecs
     *Cette étape est terminée
     * @param chess 被操作的棋子的ImageView
     *              chess ImageVue du pion manipulé
     */
    private synchronized void chessMoveUpStraightly(ImageView chess) {      //一直向上走的函数/fonction pour remonter jusqu'au bout
        while (true) {
            String r_c = (String) chessList.get(currentChessIndex.get()).getImageView().getUserData();
            // up
            int r = getR(r_c) - 1;          //向上走就是r值减一，row-1
            int c = getC(r_c);              //Monter est la valeur r moins un, ligne-1
            r_c = getR_C(r, c);

            Obstacle obstacle = obstacleMap.getOrDefault(r_c, new Obstacle(r, c));
            if (obstacle.isCanUp() && !obstacle.isHasChess()) {     //判断未来的那个点是否能向上走，若可以往上走同时那里没有别的机器人，则棋子可以走
                //Déterminez si le point dans le futur peut monter, s'il peut monter et qu'il n'y a pas d'autre robot là-bas, le pion peut monter
                chessMove(chess, r, c, r_c);        //robot移动 robot mobile
            } else {
                stepOver();
                break;
            }
        }
    }
    /**
     * 控制当前棋子一直向下移动，直至撞到墙或棋子，这一步操作结束
     * Contrôlez la pièce d'échecs actuelle pour qu'elle
     * se déplace vers le bas jusqu'à ce qu'elle touche le mur ou
     * la pièce d'échecs, cette étape est terminée
     * @param chess 被操作的棋子的ImageView
     *              chess ImageVue du pion manipulé
     */
    private synchronized void chessMoveDownStraightly(ImageView chess) {
        while (true) {      //while循环，如果棋子可以一直走下去，循环就一直进行，如果不能走了，循环break
            ///while loop, si le pion peut continuer, la boucle continuera, s'il ne peut pas continuer, la boucle se cassera
            String r_c = (String) chessList.get(currentChessIndex.get()).getImageView().getUserData();
            // down
            int r = getR(r_c) + 1;      //向下走row+1  //descendre rang+1
            int c = getC(r_c);
            r_c = getR_C(r, c);

            Obstacle obstacle = obstacleMap.getOrDefault(r_c, new Obstacle(r, c));
            if (obstacle.isCanDown() && !obstacle.isHasChess()) {
                chessMove(chess, r, c, r_c);
            } else {
                stepOver();      //机器人不能行走  Le robot ne peut pas bouger
                break;
            }
        }
    }
    /**
     * 控制当前棋子一直向左移动，直至撞到墙或棋子，这一步操作结束
     * Contrôlez la pièce d'échecs actuelle pour qu'elle se déplace vers la gauche jusqu'à
     * ce qu'elle touche le mur ou la pièce d'échecs, cette étape est terminée
     * @param chess 被操作的棋子的ImageView
     *        chess ImageVue du pion manipulé
     */
    private synchronized void chessMoveLeftStraightly(ImageView chess) {
        while (true) {
            String r_c = (String) chessList.get(currentChessIndex.get()).getImageView().getUserData();
            // left
            int r = getR(r_c);
            int c = getC(r_c) - 1;
            r_c = getR_C(r, c);

            Obstacle obstacle = obstacleMap.getOrDefault(r_c, new Obstacle(r, c));
            if (obstacle.isCanLeft() && !obstacle.isHasChess()) {
                chessMove(chess, r, c, r_c);
            } else {
                stepOver();
                break;
            }
        }
    }
    /**
     * 控制当前棋子一直向右移动，直至撞到墙或棋子，这一步操作结束
     * Contrôlez la pièce d'échecs actuelle pour qu'elle se déplace vers la
     * droite jusqu'à ce qu'elle touche le mur ou la pièce d'échecs,
     * cette étape est terminée
     * @param chess 被操作的棋子的ImageView
     *              chess ImageVue du pion manipulé
     */
    private synchronized void chessMoveRightStraightly(ImageView chess) {
        while (true) {
            String r_c = (String) chessList.get(currentChessIndex.get()).getImageView().getUserData();
            // right
            int r = getR(r_c);
            int c = getC(r_c) + 1;
            r_c = getR_C(r, c);

            Obstacle obstacle = obstacleMap.getOrDefault(r_c, new Obstacle(r, c));
            if (obstacle.isCanRight() && !obstacle.isHasChess()) {
                chessMove(chess, r, c, r_c);
            } else {
                stepOver();
                break;
            }
        }
    }

    /**
     * 结束当前步
     * terminer l'étape en cours
     *  1. 判断棋子是否抵达对应终点，如果抵达则玩家得分并直接开始下一个回合！
     *  1. Déterminez si la pièce a atteint le point final correspondant. Si c'est le cas,
     *  le joueur marquera des points et commencera directement le tour suivant！
     *  2. 如果没有抵达终点，则玩家进行下一步操作
     *  2. Si le point final n'est pas atteint, le joueur passera à l'étape suivante
     */
    private void stepOver() {
        int index = currentChessIndex.get();
        Chess chess = chessList.get(index);
        String rc = ((String) chess.getImageView().getUserData());
        if (obstacleMap.containsKey(rc) && index == obstacleMap.get(rc).finalPoint) {       //判断robot是否到达终点，并且颜色匹配
            //Déterminez si le robot a atteint le point final et si les couleurs correspondent
            int playerIndex = currentPlayerIndex.get();
            Player player = playerList.get(playerIndex);
            player.setScore(player.getScore() + 1);     //该玩家得分加1 Le score du joueur augmente de 1

            // increment !!游戏走到终点。才会让玩家的分数加一，否则永远走同一个颜色的棋子，直到棋子到达终点
            //incrément !! Le jeu touche à sa fin. Seul le score du joueur sera augmenté de un, sinon les pions de la même
            // couleur seront toujours joués jusqu'à ce que les pions atteignent la fin.
            chessOrderLabel.setText(String.valueOf(currentChessIndex.incrementAndGet() + 1));

            nextRound();        //下一个回合，下一个颜色的robot开始移动
            //Au tour suivant, le robot de la couleur suivante commence à se déplacer
        } else {
            nextStep();         //同一玩家的下一步  prochaine étape pour le même joueur
        }
    }

    /**
     * 初始化玩家步数输入面板
     * Initialiser le panneau de saisie des pas du joueur
     *  1. "Step input" 标签 [左]
     *  1. Onglet "Step input" [gauche]
     *  2. 步数输入控件       [中]
     *  2. Contrôle d'entrée de pas [Moyen]
     *  3. 确定步数按钮       [右]
     *  3.Bouton Déterminer le nombre de pas [droit]
     */
    private synchronized void initPlayerStepInputPane() {
        Label stepInputLabel = new Label("Step input");     //label
        TextField stepInputTextField = new TextField();
        stepInputButton = new Button();     //添加按钮  ajouter un bouton
        stepInputButton.setText("Next");        //给按钮设置next Définir le texte Next pour le bouton
        stepInputButton.setDisable(true);        //此时按钮是可以点击的   Le bouton est maintenant cliquable
        stepInputButton.setOnAction(new EventHandler<ActionEvent>() {
            /**
             * 确定步数按钮响应事件
             * Déterminer le bouton des étapes pour répondre aux événements
             *  1. 获取当前玩家，并更新玩家下标值
             *  1. Obtenez le lecteur actuel et mettez à jour la valeur d'indice du lecteur
             *  2. 将输入的步数信息存放到玩家对象中
             *  2. Stockez les informations d'étape saisies dans l'objet joueur
             *  3. 判断是否所有玩家已经输入完步数，如果输入完则可以开始当前回合(调用startRound)
             *  3. Déterminez si tous les joueurs ont entré le nombre de coups, s'ils sont entrés dans le tour en cours (appelez startRound)
             * @param event 按钮点击事件
             *              event clic sur un bouton événement
             */
            @Override
            public void handle(ActionEvent event) {     //点击事件，判断点击的时候是不是最后一个玩家已经输入完成，若是，则不允许点击
                //Cliquez sur l'événement pour déterminer si le dernier joueur a terminé la saisie au moment du clic.
                // Si c'est le cas, le clic n'est pas autorisé.
                int i = currentPlayerIndex.incrementAndGet();
                Player player = playerList.get(i);          //玩家号   numéro de joueur
                player.setSteps(Integer.parseInt(stepInputTextField.getText()));        //玩家输入的步数   Nombre de coups saisis par le joueur
                if (i >= playerList.size() - 1) {
                    startRound();
                } else {
                    infoLabel.setText("Turn to player " + player.getName() + " to enter number of steps");  //现在是玩家n输入步数    Maintenant c'est au joueur n d'entrer le nombre de coups
                }
            }
        });
        HBox hbox = new HBox(8); // spacing = 8
        hbox.getChildren().addAll(stepInputLabel, stepInputTextField, stepInputButton);

        rightRoot.getChildren().add(hbox);
    }

    /**
     * 开始当前回合
     * commencer le tour en cours
     *  1. 按照玩家输入的步数排序
     *  1. Trier par le nombre de coups saisis par le joueur
     *  2. 重置当前玩家下标号
     *  2. Réinitialiser l'indice du joueur actuel
     *  3. 开始由步数设定最小的玩家开始操作(调用nextPlayerStep)
     *  3. Démarrez l'opération par le joueur avec le plus petit nombre d'étapes (appelez nextPlayerStep)
     */
    private void startRound() {
        // sort by steps
        playerList.sort(new Player.PlayerStepsComparator());
        currentPlayerIndex.set(-1);
        stepInputButton.setDisable(true);
//        System.out.println(playerList.get(0).getSteps());
        nextPlayerStep();
    }

    /**
     * 由下一个玩家操作
     * Actionné par le joueur suivant
     *  1. 重置当前棋子位置(将棋子移动到原本生成的位置)
     *  1. Réinitialisez la position actuelle du pion (déplacez le pion à la position générée d'origine)
     *  2. 更新并获取当前玩家下标
     *  2. Mettez à jour et obtenez l'indice du joueur actuel
     *  3. 判断当前回合是否所有玩家都已经消耗完步数
     *  3. Déterminez si tous les joueurs du tour en cours ont épuisé leurs pas
     *      - 如果消耗完了，则重新开始新的回合(当前操作的棋子不变)
     *      - S'il est épuisé, recommencez un nouveau tour (les pièces d'échecs actuellement utilisées restent inchangées)
     *      - 没有消耗完，则轮到下一个 设定步数 最少的玩家操作
     *      - S'il n'est pas epuise, ce sera la prochaine opération du joueur avec le nombre minimum d'étapes définies
     */
    private synchronized void nextPlayerStep() {
        int chessIndex = currentChessIndex.get();
        if (chessIndex >= 0 && chessIndex < chessList.size()) {
            Chess chess = chessList.get(chessIndex);
            String originRC = chess.getOriginRC();
            chessMove(chess.getImageView(), getR(originRC), getC(originRC), originRC);
        }


        int index = currentPlayerIndex.incrementAndGet();
        if (index >= playerList.size()) {
            // round over
            nextRound();
            return;
        }

        Player player = playerList.get(index);
        player.setUsedSteps(0);
        // start round
        currentStepTime.set(STEP_TIME);
        timeInfoLabel.setText(String.valueOf(STEP_TIME));
        moveTimer.start();

        nextStep();
    }

    /**
     * 下一步玩家控制棋子移动操作
     * À l'étape suivante, le joueur contrôle le mouvement des pièces
     *   1. 获取当前玩家是否还剩余步数(player.getUsedSteps() >= player.getSteps())
     *   1. Obtenir le nombre de pas laissés par le joueur actuel (player.getUsedSteps() >= player.getSteps())
     *      - 如果使用完了步数，则轮到下一个玩家操作(调用nextPlayerStep)
     *      - Si le nombre de pas est épuisé, c'est au tour du joueur suivant (appelez nextPlayerStep)
     *   2. 更新游戏状态标签，通知当前玩家还剩下多少步
     *   2. Mettez à jour l'étiquette d'état du jeu pour informer le joueur actuel du nombre de coups restants
     *   3. 更新玩家使用的步数，步数+1
     *   3. Mettre à jour le nombre de pas utilisés par le joueur, le nombre de pas +1
     */
    private synchronized void nextStep() {
        int index = currentPlayerIndex.get();
        Player player = playerList.get(index);
        System.out.println("nextStep,steps:"+player.getSteps()+",used:"+player.getUsedSteps());
        if (player.getUsedSteps() >= player.getSteps()) {
            // next player step
            nextPlayerStep();
            return;
        }

        infoLabel.setText("Player " + player.getName() + " has " + (player.getSteps() - player.getUsedSteps()) + " steps left.");
        player.setUsedSteps(player.getUsedSteps() + 1);
    }

    /**
     * 下一个回合
     * tour suivant
     *  1. 将定时器暂停，并重置剩余时间
     *  1. Mettez le chronomètre en pause et réinitialisez le temps restant
     *  2. 判断当前是否所有棋子都已经抵达终点(游戏是否结束)
     *  2. Déterminez si toutes les pièces ont atteint la fin (si le jeu est terminé)
     *      - 如果所有棋子都已经抵达终点，则结束游戏(调用gameOver)
     *      - Si toutes les pièces ont atteint la fin, terminez le jeu (appelez gameOver)
     *  3. 重置当前玩家号
     *  3. Réinitialiser le numéro de joueur actuel
     *  4. 更新游戏状态标签，通知玩家开始输入步数
     *  4. Mettez à jour la balise d'état du jeu pour informer le joueur de commencer à saisir le nombre de coups
     *  5. 设置步数输入确定按钮可以点击
     *  5. Définissez le nombre d'étapes à saisir et le bouton OK peut être cliqué
     */
    private void nextRound() {
        moveTimer.stop();
        currentStepTime.set(STEP_TIME);

        // no increment!!!
        int chessIndex = currentChessIndex.get();
        if (chessIndex >= chessList.size()) {
            gameOver();
            return;
        }
        currentPlayerIndex.set(-1);

        infoLabel.setText("Turn to player " + playerList.get(0).getName() + " to enter number of steps");
        stepInputButton.setDisable(false);
    }

    /**
     * 游戏结束
     *  1. 更新游戏状态标签为 "Waiting game start"
     *  1. Mettez à jour la balise d'état du jeu sur "Waiting game start"
     *  2. 重置当前玩家号
     *  2. Réinitialiser le numéro de joueur actuel
     *  3. 按照玩家分数进行排序
     *  3. Trier par score de joueur
     *  4. 弹窗显示获胜玩家，以及各个玩家的得分情况
     *  4. La fenêtre contextuelle affiche les joueurs gagnants et le score de chaque joueur
     */
    private void gameOver() {
        System.err.println("Game Over！");
        infoLabel.setText("Waiting game start");
        currentChessIndex.set(-1);

        playerList.sort(new Player.PlayerScoreComparator());
        StringBuilder text = new StringBuilder("Game Over ! Player " + playerList.get(0).getName() + " win!\n");

        for (Player player : playerList) {
            text.append("Player ").append(player.getName()).append(" : ").append(player.getScore()).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text.toString());
        alert.showAndWait();
    }

    private void initPlayerOrderPane() {

    }

    /**
     * 初始化棋子顺序面板
     * Initialiser le panneau de commande d'échecs
     *  1. 创建四个棋子按钮，绑定点击事件 -> 刷新对应棋子位置，并重置回合
     *  * 1. Créez quatre boutons de pion, liez l'événement de clic
     *  -> actualisez la position de pion correspondante et réinitialisez le tour
     *  2. 创建一个当前被操作棋子的提示标签，通过数字进行区分被操作的棋子
     *  2. Créez une étiquette d'invite pour la pièce actuellement opérée et distinguez les pièces opérées par des numéros
     */
    private void initChessOrderPane() {      //棋子顺序已经设定好 L'ordre des pièces a été fixé red：1， green：2， yellow：3， blue：4
        Button red = new Button("1");       //生成四个按钮    Générer quatre boutons
        Button green = new Button("2");
        Button yellow = new Button("3");
        Button blue = new Button("4");

        red.setGraphic(new ImageView(redChessImage));       //给每个按钮设置背景图片   Définir une image d'arrière-plan pour chaque bouton
        green.setGraphic(new ImageView(greenChessImage));
        yellow.setGraphic(new ImageView(yellowChessImage));
        blue.setGraphic(new ImageView(blueChessImage));

        red.setOnAction(event -> {          //给按钮设置点击事件 Définir l'événement de clic pour le bouton
            randomChessPos(chessList.get(0));
            nextRound();
        });
        green.setOnAction(event -> {
            randomChessPos(chessList.get(1));       //只重置这一个按钮的位置并且 ne réinitialise que la position de ce bouton et
            nextRound();                            //开启下一个回合   commencer le tour suivant
        });
        yellow.setOnAction(event -> {
            randomChessPos(chessList.get(2));
            nextRound();
        });
        blue.setOnAction(event -> {
            randomChessPos(chessList.get(3));
            nextRound();
        });

        HBox hbox = new HBox(8); // spacing = 8      //设置水平布局   définir la disposition horizontale
        hbox.getChildren().addAll(red, green, yellow, blue, chessOrderLabel);

        rightRoot.getChildren().add(hbox);          //然后将这个布局加到总的布局里面   Ajoutez ensuite cette mise en page à la mise en page générale
    }

    /**
     * 初始化玩家数量输入面板  Initialiser le panneau de saisie du nombre de joueurs
     *  1. "PlayerNumber" 玩家数量提示标签  [左] "PlayerNumber" Indicateur de nombre de joueurs [gauche]
     *  2. 玩家数量输入控件，默认为2         [中]    Contrôle d'entrée du nombre de joueurs, la valeur par défaut est 2 [Moyen]
     *  3. 确定玩家数量，开始游戏按钮         [右]    Déterminez le nombre de joueurs, démarrez le bouton de jeu [à droite]
     */
    private void initPlayerNumberPane() {
        Label playerNumberLabel = new Label("PlayerNumber");
        TextField playerNumberTextField = new TextField();
        playerNumberTextField.setText("2");
        Button playerNumberBtn = new Button();
        playerNumberBtn.setText("Start");
        playerNumberBtn.setOnAction(new EventHandler<ActionEvent>() {       //点击函数，点击按钮 Cliquez sur la fonction, cliquez sur le bouton
            @Override
            public void handle(ActionEvent event) {
                startGame(Integer.parseInt(playerNumberTextField.getText()));
            }   //传入playerNumberTextField，默认是2.获取玩家数量，传入开始游戏
            //Passez playerNumberTextField, la valeur par défaut est 2. Obtenez le nombre de joueurs, passez pour commencer le jeuPassez playerNumberTextField,
            // la valeur par défaut est 2. Obtenez le nombre de joueurs, passez pour commencer le jeu
        });
        HBox hbox = new HBox(8); // spacing = 8
        hbox.getChildren().addAll(playerNumberLabel, playerNumberTextField, playerNumberBtn);

        rightRoot.getChildren().add(hbox);
    }

    /**
     * 初始化游戏地图以及相关控件和事件
     * Initialiser la carte du jeu et les commandes et événements associés
     *  1. 初始化地图
     *  1. Initialiser la carte
     *  2. 初始化相关控件和事件
     *  2. Initialiser les contrôles et événements associés
     *      1. 初始化障碍物Map集合
     *      1. Initialiser la collection de cartes d'obstacles
     *      2. 初始化非出生点Set集合
     *      2. Initialiser la collection Set non-spawn
     *      3. 初始化棋子
     *      3. Initialiser les pièces
     *      4. 初始化终点(也是放在障碍物Map集合中)
     *      4. Initialiser le point final (également placé dans la collection Obstacle Map)
     *      5. 初始化键盘监听事件
     *      5. Initialiser les événements d'écoute du clavier
     */
    private void initGameGridPane() {       //绘制初始的棋盘   Dessinez le premier damier
        GridPane gridPane = new GridPane();
        for (int i = 0; i < SIZE; i++) {
            ColumnConstraints columnConstraints = new ColumnConstraints();
            columnConstraints.setPercentWidth(100 / (SIZE * 1.0));  //每列宽   largeur de chaque colonne
            gridPane.getColumnConstraints().add(columnConstraints);
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setPercentHeight(100 / (SIZE * 1.0));    //每行高   hauteur de la ligne
            gridPane.getRowConstraints().add(rowConstraints);
        }
        gridPane.setPadding(new Insets(PADDING));

        initObstacle();     //mur
        initNonBirthPoint();        //place can not birth
        initChess(gridPane);        //robot
        initFinalPoint();           //final point
        initKeyBoardEvent();        //clavier

        gridPane.setPrefSize(WIDTH, HEIGHT);

        Image image = new Image(getClass().getResourceAsStream("/gui/bg.jpg"));
        BackgroundSize size = new BackgroundSize(WIDTH, HEIGHT, true, false, false, true);
        gridPane.setBackground(new Background(new BackgroundImage(image, null, null, null, size)));

        root.getChildren().add(gridPane);
    }

    /**
     * 初始化终点集合  Initialiser la collection de points de terminaison
     *  将终点信息标记到 障碍物集合中 Marquez les informations de fin dans l'ensemble d'obstacles
     */
    private void initFinalPoint() {
        // red 0
        addFinalPointToMap(0, 4, 2);
        addFinalPointToMap(0, 3, 11);
        addFinalPointToMap(0, 14, 4);
        addFinalPointToMap(0, 11, 12);
        // green 1
        addFinalPointToMap(1, 5, 4);
        addFinalPointToMap(1, 6, 10);
        addFinalPointToMap(1, 12, 1);
        addFinalPointToMap(1, 11, 13);
        // yellow 2
        addFinalPointToMap(2, 2, 3);
        addFinalPointToMap(2, 1, 9);
        addFinalPointToMap(2, 9, 3);
        addFinalPointToMap(2, 9, 10);
        // blue 3
        addFinalPointToMap(3, 3, 5);
        addFinalPointToMap(3, 5, 14);
        addFinalPointToMap(3, 11, 6);
        addFinalPointToMap(3, 13, 9);
    }

    /**
     * 将终点信息标记到障碍物集合的辅助方法
     * Méthode d'assistance pour marquer les informations de point final sur l'ensemble d'obstacles
     * @param finalPoint    终点值 valeur finale
     * @param r 行号  numéro de ligne
     * @param c 列号  numéro de colonne
     */
    private void addFinalPointToMap(int finalPoint, int r, int c) {
        String rc = getR_C(r, c);
        Obstacle obstacle = obstacleMap.getOrDefault(rc, new Obstacle(r, c));
        obstacle.setFinalPoint(finalPoint);
        obstacleMap.put(rc, obstacle);
    }

    /**
     * 初始化障碍物Map集合  Initialiser la collection de cartes d'obstacles
     *  1. 将地图四周设置不可通行点放入Map集合      1. Placez les points infranchissables autour de la carte dans la collection Map
     *  2. 一行一行设置不可通行的障碍物点，放入Map集合      Définissez les points d'obstacles infranchissables ligne par ligne et mettez-les dans la collection Map
     */
    private void initObstacle() {       //有墙的格子     treillis avec murs
        for (int i = 0; i < 16; i++) {
            obstacleMap.put(getR_C(-1, i), new Obstacle(-1, i, false, false, false, false, false));
            obstacleMap.put(getR_C(16, i), new Obstacle(16, i, false, false, false, false, false));
            obstacleMap.put(getR_C(i, 16), new Obstacle(i, 16, false, false, false, false, false));
            obstacleMap.put(getR_C(i, -1), new Obstacle(i, -1, false, false, false, false, false));
        }       //设置一个墙把棋盘围住，这样robot就无法走出去      Installez un mur pour entourer le tableau afin que le robot ne puisse pas sortir

        // row 0
        obstacleMap.put(getR_C(0, 5), new Obstacle(0, 5, true, true, false, true, false));
        obstacleMap.put(getR_C(0, 6), new Obstacle(0, 6, true, true, true, false, false));

        obstacleMap.put(getR_C(0, 11), new Obstacle(0, 11, true, true, false, true, false));
        obstacleMap.put(getR_C(0, 12), new Obstacle(0, 12, true, true, true, false, false));
        // row 1
        obstacleMap.put(getR_C(1, 3), new Obstacle(1, 3, false, true, true, true, false));

        obstacleMap.put(getR_C(1, 9), new Obstacle(1, 9, false, true, false, true, false));
        obstacleMap.put(getR_C(1, 10), new Obstacle(1, 10, true, true, true, false, false));
        // row 2
        obstacleMap.put(getR_C(2, 2), new Obstacle(2, 2, true, true, false, true, false));
        obstacleMap.put(getR_C(2, 3), new Obstacle(2, 3, true, false, true, false, false));

        obstacleMap.put(getR_C(2, 9), new Obstacle(2, 9, true, false, true, true, false));

        obstacleMap.put(getR_C(2, 15), new Obstacle(2, 15, false, true, true, true, false));
        // row 3
        obstacleMap.put(getR_C(3, 0), new Obstacle(3, 0, false, true, true, true, false));

        obstacleMap.put(getR_C(3, 2), new Obstacle(3, 2, false, true, true, true, false));

        obstacleMap.put(getR_C(3, 4), new Obstacle(3, 4, true, true, false, true, false));
        obstacleMap.put(getR_C(3, 5), new Obstacle(3, 5, false, true, true, false, false));

        obstacleMap.put(getR_C(3, 10), new Obstacle(3, 10, true, true, false, true, false));
        obstacleMap.put(getR_C(3, 11), new Obstacle(3, 11, false, true, true, false, false));

        obstacleMap.put(getR_C(3, 15), new Obstacle(3, 15, true, false, true, true, false));
        // row 4
        obstacleMap.put(getR_C(4, 0), new Obstacle(4, 0, true, false, true, true, false));

        obstacleMap.put(getR_C(4, 2), new Obstacle(4, 2, true, false, false, true, false));
        obstacleMap.put(getR_C(4, 3), new Obstacle(4, 3, true, true, true, false, false));

        obstacleMap.put(getR_C(4, 5), new Obstacle(4, 5, true, false, true, true, false));

        obstacleMap.put(getR_C(4, 11), new Obstacle(4, 11, true, false, true, true, false));

        obstacleMap.put(getR_C(4, 14), new Obstacle(4, 14, false, true, true, true, false));
        // row 5
        obstacleMap.put(getR_C(5, 4), new Obstacle(5, 4, false, true, false, true, false));

        obstacleMap.put(getR_C(5, 10), new Obstacle(5, 10, false, true, true, true, false));

        obstacleMap.put(getR_C(5, 13), new Obstacle(5, 13, true, true, false, true, false));
        obstacleMap.put(getR_C(5, 14), new Obstacle(5, 14, true, false, true, false, false));
        // row 6
        addObstacleToMap(6, 4,"d");

        addObstacleToMap(6, 10, "dl");
        addObstacleToMap(6, 11, "r");
        // row 7
        addObstacleToMap(7, 7, "udlr");
        addObstacleToMap(7, 8, "udlr");

        addObstacleToMap(7, 12, "l");
        addObstacleToMap(7, 13, "ru");
        // row 8
        addObstacleToMap(8, 3, "u");

        addObstacleToMap(8, 7, "udlr");
        addObstacleToMap(8, 8, "udlr");
        addObstacleToMap(8, 13, "d");
        // row 9
        addObstacleToMap(9, 3, "dl");
        addObstacleToMap(9, 4, "r");

        addObstacleToMap(9, 10, "ul");
        addObstacleToMap(9, 11, "r");
        // row 10
        addObstacleToMap(10, 6, "u");

        addObstacleToMap(10 , 10, "d");

        addObstacleToMap(10, 12, "u");
        // row 11
        addObstacleToMap(11, 5, "l");
        addObstacleToMap(11, 6,"rd");

        addObstacleToMap(11, 12, "dl");
        addObstacleToMap(11,13,"ru");
        // row 12
        addObstacleToMap(12, 0 ,"l");
        addObstacleToMap(12,1,"ru");

        addObstacleToMap(12, 9,"udlr");

        addObstacleToMap(12,15,"u");
        // row 13
        addObstacleToMap(13,0,"u");

        addObstacleToMap(13, 1, "d");

        addObstacleToMap(13,8,"l");
        addObstacleToMap(13,9,"rd");

        addObstacleToMap(13,15,"d");
        // row 14
        addObstacleToMap(14,0,"d");

        addObstacleToMap(14,4,"lu");
        addObstacleToMap(14,5,"r");

        addObstacleToMap(14,11,"udlr");
        // row 15
        addObstacleToMap(15,4,"d");

        addObstacleToMap(15,6,"l");
        addObstacleToMap(15,7,"r");

        addObstacleToMap(15,10,"l");
        addObstacleToMap(15,11,"r");



    }

    /**
     * 初始化非出生点Set集合     Initialiser la collection Set non-spawn
     *  将不允许棋子刷新的位置点放入到一个Set集合中     Placez les points de position qui ne permettent pas au pion de se rafraîchir dans une collection Set
     */
    private void initNonBirthPoint() {
        nonBirthPointSet.add(getR_C(1,9));
        nonBirthPointSet.add(getR_C(2,3));
        nonBirthPointSet.add(getR_C(3,5));
        nonBirthPointSet.add(getR_C(3,11));
        nonBirthPointSet.add(getR_C(4,2));
        nonBirthPointSet.add(getR_C(5,4));
        nonBirthPointSet.add(getR_C(5,14));
        nonBirthPointSet.add(getR_C(6,10));
        nonBirthPointSet.add(getR_C(7,7));
        nonBirthPointSet.add(getR_C(7,8));
        nonBirthPointSet.add(getR_C(7,13));
        nonBirthPointSet.add(getR_C(8,7));
        nonBirthPointSet.add(getR_C(8,8));
        nonBirthPointSet.add(getR_C(9,3));
        nonBirthPointSet.add(getR_C(9,10));
        nonBirthPointSet.add(getR_C(11,6));
        nonBirthPointSet.add(getR_C(11,12));
        nonBirthPointSet.add(getR_C(11,13));
        nonBirthPointSet.add(getR_C(12,1));
        nonBirthPointSet.add(getR_C(12,9));
        nonBirthPointSet.add(getR_C(13,9));
        nonBirthPointSet.add(getR_C(14,4));
        nonBirthPointSet.add(getR_C(14,11));
    }

    /**
     * 随机刷新指定棋子位置       Rafraîchir au hasard la position de la pièce d'échecs spécifiée
     *  1. 随机生成行与列，直到生成的坐标允许放入棋子时，将该棋子移动至该坐标
     *  1. Générez aléatoirement des lignes et des colonnes jusqu'à ce que les coordonnées générées permettent
     *  le placement d'une pièce d'échecs, déplacez la pièce d'échecs à cette coordonnée
     *  2. 更新棋子原始位置(originRC)
     *  2. Mettre à jour la position d'origine du pion (originRC)
     * @param chess 被刷新位置的棋子    Pion en position rafraîchie
     */
    private synchronized void randomChessPos(Chess chess) {     //synchronized线程同步  synchronisation des threads
        Random random = new Random();       //随机数   nombre aléatoire
        int r;      //row ligne
        int c;      //colome
        String rc;
        do{
            r = Math.abs(random.nextInt()) % 16;        //用随机数生成随机的位置，因为随机数有可能生成负数。所以用abs取随机数的绝对值
            //Utilisez des nombres aléatoires pour générer des positions aléatoires, car les nombres aléatoires peuvent générer des nombres négatifs.
            // Utilisez donc abs pour prendre la valeur absolue du nombre aléatoire
            c = Math.abs(random.nextInt()) % 16;        //对16进行取余，因为棋盘是16*16
            //Prenez le reste de 16 car le plateau est de 16*16
            rc = getR_C(r, c);      //得到随机的位置   obtenir un emplacement aléatoire
        }while (nonBirthPointSet.contains(rc) || obstacleMap.containsKey(rc) && obstacleMap.get(rc).hasChess);
        //得到位置要进行判断，判断不能是出生点的地方（终点，中间四个格子，障碍物（两个斜着的））||判断是否有棋子
        //L'emplacement doit être jugé, et le jugement ne peut pas être le lieu du point de naissance (le point final,
        // les quatre carrés du milieu, les obstacles (deux obliques)) || juger s'il y a des pièces d'échecs
        chessMove(chess.getImageView(), r, c, rc);//移动棋子
        chess.setOriginRC(rc);
        //在chess类里面设置的原始的位置，记录原始位置是为了一个人操作失败之后，下一个玩家可以回到之前的位置开始进行操作
        //La position d'origine définie dans la classe de chess, la position d'origine est enregistrée de sorte
        // qu'après l'échec de l'opération d'une personne, le joueur suivant peut revenir à la position précédente pour commencer l'opération
    }

    /**
     * 移动棋子
     * déplacer le pion
     *  1. 获取当前棋子坐标
     *  1. Obtenir les coordonnées de la pièce en cours
     *  2. 将棋子移动到指定坐标
     *  2. Déplacez le pion aux coordonnées spécifiées
     *      - 将当前点标记为无棋子
     *      - marquer le point actuel comme aucun pion
     *      - 将目标点标记为有棋子
     *      - marquer les points cibles comme ayant des pions
     * @param chess 被移动的棋子的ImageView    ImageVue du pion déplacé
     * @param r 移动的目标点行号    Numéro de ligne de point cible déplacé
     * @param c 移动的目标点列号    Numéro de colonne de point cible déplacé
     * @param rc    移动的目标点坐标字符串     La chaîne de coordonnées du point cible mobile
     */
    private synchronized void chessMove(ImageView chess, int r, int c, String rc) {
        String lastRC = (String) chess.getUserData();
        obstacleMap.get(lastRC).setHasChess(false);

        GridPane.setConstraints(chess, c, r);

        chess.setUserData(rc);

        Obstacle obstacle = obstacleMap.getOrDefault(rc, new Obstacle(r, c));
        obstacle.setHasChess(true);
        obstacleMap.put(rc, obstacle);
    }

    /**
     * 随机刷新所有棋子位置       Rafraîchir au hasard toutes les positions des pions
     */
    private void randomAllChessPos(){
        for (Chess chess : chessList) {
            randomChessPos(chess);      //对所有的棋子进行遍历，遍历完成之后对每一个棋子调用一次随机放置
        }
    }

    /**
     * 添加障碍物点到Map集合的辅助方法
     * Méthode d'assistance pour ajouter des points d'obstacle à la collection Map
     * @param row   行号  numéro de ligne
     * @param col   列号  numéro de colonne
     * @param re    不允许进入该点的方式 字符串 (包含该点是否为棋子)
     *              Chemins non autorisés à saisir le point Chaîne (contient si le point est un pion)
     *              - u : 不允许下方点直接抵达该点  Le point ci-dessous n'est pas autorisé à aller directement à ce point
     *              - d : 不允许上方点直接抵达该点  Le point ci-dessus n'est pas autorisé à atteindre le point directement
     *              - l : 不允许右方点直接抵达该点  Le point de droite n'est pas autorisé à aller directement à ce point
     *              - r : 不允许左方点直接抵达该点  Le point de gauche n'est pas autorisé à aller directement à ce point
     *              - c : 标记该点有棋子       Marquez le point avec un pion
     */
    private void addObstacleToMap(int row, int col, String re) {
        boolean l = true, r = true, u = true, d = true, chess = false;
        for (int i = 0; i < re.length(); i++) {
            if (re.charAt(i) == 'l') {
                l = false;
            } else if (re.charAt(i) == 'r') {
                r = false;
            } else if (re.charAt(i) == 'u') {
                u = false;
            } else if (re.charAt(i) == 'd') {
                d = false;
            } else if (re.charAt(i) == 'c') {
                chess = true;
            }
        }
        obstacleMap.put(getR_C(row, col), new Obstacle(row, col, u, d, l, r, chess));
    }

    /**
     * 初始化四个棋子  Initialiser quatre pions
     * @param gridPane  棋盘  damier
     */
    private void initChess(GridPane gridPane) {
        chessList = new ArrayList<Chess>();
        redChess = addRobot(gridPane, redChessImage, 1, 1);
        chessList.add(new Chess(redChess));
        greenChess = addRobot(gridPane, greenChessImage, 8, 9);
        chessList.add(new Chess(greenChess));
        yellowChess = addRobot(gridPane, yellowChessImage, 13, 5);
        chessList.add(new Chess(yellowChess));
        blueChess = addRobot(gridPane, blueChessImage, 4, 13);
        chessList.add(new Chess(blueChess));
    }

    /**
     * 添加棋子(机器人)到棋盘上的指定位置      Ajouter des pièces d'échecs (robots) à des positions spécifiées sur l'échiquier
     * @param gridPane  棋盘  damier
     * @param image 棋子图片    Photo de robot
     * @param row   行号      numéro de ligne
     * @param col   列号      numéro de colonne
     * @return  生成的棋子ImageView  Généré pion ImageView
     */
    private ImageView addRobot(GridPane gridPane, Image image, int row, int col) {
        ImageView imageView = new ImageView(image);
        imageView.setUserData(row + "_" + col);

        gridPane.add(imageView, col, row);
        String r_c = getR_C(row, col);
        Obstacle obstacle = obstacleMap.getOrDefault(r_c, new Obstacle(row, col));
        obstacle.setHasChess(true);
        obstacleMap.put(r_c, obstacle);
        return imageView;
    }

    /**
     * 开始游戏
     * commencer le jeu
     *  1. 根据输入的玩家数量，初始化玩家列表
     *  1. Initialiser la liste des joueurs en fonction du nombre de joueurs saisis
     *  2. 设置步数输入按钮为可点击的
     *  2. Rendre le bouton d'entrée pas à pas cliquable
     *  3. 更新游戏状态标签为轮到首个玩家去输入步数
     *  3. Mettez à jour l'étiquette d'état du jeu au tour du premier joueur pour entrer le nombre de coups
     * @param playerNumber 玩家数量 Nombre de joueurs
     */
    private synchronized void startGame(int playerNumber) {
        randomAllChessPos();
        currentPlayerIndex.set(-1);
        currentChessIndex.set(0);
        playerList = new ArrayList<>(playerNumber);
        for (int i = 0; i < playerNumber; i++) {
            Player p = new Player();
            p.setName(String.valueOf(i + 1));
            playerList.add(p);
        }
        stepInputButton.setDisable(false);
        infoLabel.setText("Turn to player " + playerList.get(0).getName() + " to input steps");
    }

    /**
     * 主方法，程序入口
     * méthode principale, entrée de programme
     *  1. 设置本地语言为 英语 (保证弹窗等内容为英文)
     *  1. Définissez la langue locale sur l'anglais
     *  (assurez-vous que la fenêtre contextuelle et les autres contenus sont en anglais)
     *  2. 调用launch运行游戏     Lancement d'appel pour lancer le jeu
     * @param args
     */
    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        launch();
    }

}