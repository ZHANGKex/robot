package gui;

import java.util.Comparator;

/**
 * 玩家类  Classe de player
 *  用于记录玩家信息    Utilisé pour enregistrer les informations du joueur
 */
public class Player {
    /**
     * 当前回合玩家设定的步数
     * Le nombre de pas défini par le joueur dans le tour en cours
     */
    private int steps;
    /**
     * 当前回合玩家已经消耗的步数
     * Le nombre de pas que le joueur a consommés dans le tour en cours
     */
    private int usedSteps;
    /**
     * 本局游戏中玩家得分
     * Les joueurs marquent dans ce jeu
     *  帮助机器人抵达终点得一分
     *  Obtenez un point pour avoir aidé le robot à atteindre la ligne d'arrivée
     */
    private int score;
    /**
     * 玩家名称(用于区分玩家)
     * Nom du joueur (utilisé pour distinguer les joueurs)
     *  默认为 1,2,...
     *  Par défaut à 1, 2,...
     */
    private String name;

    /**
     * 按照玩家设定的步数上限进行排序
     * Trier par nombre maximum de coups défini par le joueur
     *  步数(steps)设定越少排在越前面
     *  Moins le nombre d'étapes est défini, plus il y en a à l'avant
     */
    public static class PlayerStepsComparator implements Comparator<Player> {
        @Override
        public int compare(Player o1, Player o2) {
            if (o1.steps == o2.steps) {
                double v = Math.random();
                if (v < 0.34) {
                    return 0;
                } else if (v < 0.67) {
                    return 1;
                } else {
                    return -1;
                }
            }
            return o1.steps - o2.steps;
        }
    }

    /**
     * 按照玩家的分数进行排序
     * Trier par score de joueur
     *  分数(score)越高排在越前面
     *  Plus le score est élevé, plus le classement est élevé
     */
    public static class PlayerScoreComparator implements Comparator<Player> {
        @Override
        public int compare(Player o1, Player o2) {
            if (o1.score == o2.score) {
                double v = Math.random();
                if (v < 0.34) {
                    return 0;
                } else if (v < 0.67) {
                    return 1;
                } else {
                    return -1;
                }
            }
            return o2.score - o1.score;
        }
    }

    /**
     * --------------------------------------
     * 构造器
     * Constructeur
     * --------------------------------------
     */
    public Player() {
    }

    /**
     * --------------------------------------
     * Getter Setter
     * --------------------------------------
     */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getUsedSteps() {
        return usedSteps;
    }

    public void setUsedSteps(int usedSteps) {
        this.usedSteps = usedSteps;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
