package org.example;

import java.util.*;

public class KnightToCastle {

    static class Position {
        int row, col, moves;

        public Position(int row, int col, int moves) {
            this.row = row;
            this.col = col;
            this.moves = moves;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Position position = (Position) obj;
            return row == position.row && col == position.col;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
    }

    private static final int[][] KNIGHT_MOVES = {
        {2, 1}, {1, 2}, {-1, 2}, {-2, 1},
        {-2, -1}, {-1, -2}, {1, -2}, {2, -1}
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String playAgain;
        do {
            System.out.print("Enter the size of the board (N >= 5): ");
            int N = scanner.nextInt();
            if (N < 5) {
                System.out.println("Error: Board size must be at least 5.");
                return;
            }

            int[][] board = new int[N][N];
            Set<Integer> usedIDs = new HashSet<>();
            Random rand = new Random();

            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    int id;
                    do {
                        id = rand.nextInt(100) + 1;
                    } while (usedIDs.contains(id));
                    board[i][j] = id;
                    usedIDs.add(id);
                }
            }

            System.out.println("Chessboard with Random IDs:");
            printBoard(board);

            Position knight = null;
            while (knight == null) {
                System.out.print("Enter the ID of the Knight's starting position: ");
                int knightID = scanner.nextInt();
                knight = findPositionByID(board, knightID);
                if (knight == null) {
                    System.out.println("Invalid Knight's ID. Please try again.");
                }
            }

            Position castle = null;
            while (castle == null || knight.row == castle.row && knight.col == castle.col) {
                System.out.print("Enter the ID of the Castle's position: ");
                int castleID = scanner.nextInt();
                castle = findPositionByID(board, castleID);
                if (castle == null) {
                    System.out.println("Invalid Castle's ID. Please try again.");
                } else if (castle.row == knight.row && castle.col == knight.col) {
                    System.out.println("Knight and Castle cannot occupy the same position. Please choose another ID.");
                    castle = null;
                }
            }

            Set<Position> bombPositions = new HashSet<>();
            while (true) {
                System.out.print("Enter the IDs of the bombs (comma-separated): ");
                String bombIDs = scanner.next();
                String[] bombIDsArray = bombIDs.split(",");
                boolean valid = true;

                for (String idStr : bombIDsArray) {
                    int id = Integer.parseInt(idStr.trim());
                    Position bomb = findPositionByID(board, id);
                    if (bomb == null) {
                        System.out.println("Invalid Bomb ID: " + id);
                        valid = false;
                    } else if (bomb.row == knight.row && bomb.col == knight.col) {
                        System.out.println("Bomb cannot be placed on the Knight's position.");
                        valid = false;
                    } else if (bomb.row == castle.row && bomb.col == castle.col) {
                        System.out.println("Bomb cannot be placed on the Castle's position.");
                        valid = false;
                    } else {
                        bombPositions.add(bomb);
                    }
                }

                if (valid) break;
            }

            System.out.println("\nChessboard after placing the Knight (K), Castle (C), and Bombs (B):");
            System.out.println("Initial -> knight is at (" + board[knight.row][knight.col] + ")");
            printBoardWithKnightCastleAndBombs(board, bombPositions, knight, castle);

            boolean[][] bombBoard = new boolean[N][N];
            for (Position bomb : bombPositions) {
                bombBoard[bomb.row][bomb.col] = true;
            }

            List<Position> path = new ArrayList<>();
            int result = bfs(N, bombBoard, knight, castle, path);

            if (result == -1) {
                System.out.println("It is not possible for the Knight to reach the Castle.");
            } else {
                System.out.println("Minimum moves for the Knight to reach the Castle: " + result);
                System.out.println("Tracking each move:");
                trackMoves(board, path, bombPositions, castle);
            }

            System.out.print("\nDo you want to play again? (Y/y for yes, others for no): ");
            playAgain = scanner.next();
        } while (playAgain.equalsIgnoreCase("Y"));

        System.out.println("Thank you for playing!");
    }

    private static Position findPositionByID(int[][] board, int ID) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == ID) {
                    return new Position(i, j, 0);
                }
            }
        }
        return null;
    }

    private static void printBoard(int[][] board) {
        for (int[] row : board) {
            for (int id : row) {
                System.out.print(id + "\t");
            }
            System.out.println();
        }
    }

    private static void printBoardWithKnightCastleAndBombs(int[][] board, Set<Position> bombPositions, Position knight, Position castle) {
        int cellWidth = 8; // Adjust this to suit your needs.
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                final int row = i;
                final int col = j;
                String content;
                if (knight.row == i && knight.col == j && castle.row == i && castle.col == j) {
                    content = board[i][j] + "*C+K";
                } else if (knight.row == i && knight.col == j) {
                    content = board[i][j] + "*K";
                } else if (castle.row == i && castle.col == j) {
                    content = board[i][j] + "*C";
                } else if (bombPositions.stream().anyMatch(b -> b.row == row && b.col == col)) {
                    content = board[i][j] + "*B";
                } else {
                    content = String.valueOf(board[i][j]);
                }
                // Format each cell to align within the specified width
                System.out.print(String.format("%-" + cellWidth + "s", content));
            }
            System.out.println();
        }
    }

    private static int bfs(int N, boolean[][] bombBoard, Position knight, Position castle, List<Position> path) {
        Queue<Position> queue = new LinkedList<>();
        boolean[][] visited = new boolean[N][N];
        Map<Position, Position> parent = new HashMap<>();

        queue.add(knight);
        visited[knight.row][knight.col] = true;

        while (!queue.isEmpty()) {
            Position current = queue.poll();
            if (current.row == castle.row && current.col == castle.col) {
                Position temp = current;
                while (temp != null) {
                    path.add(0, temp);
                    temp = parent.get(temp);
                }
                return current.moves;
            }

            for (int[] move : KNIGHT_MOVES) {
                int newRow = current.row + move[0];
                int newCol = current.col + move[1];

                if (isValidMove(N, bombBoard, visited, newRow, newCol)) {
                    visited[newRow][newCol] = true;
                    Position next = new Position(newRow, newCol, current.moves + 1);
                    queue.add(next);
                    parent.put(next, current);
                }
            }
        }
        return -1;
    }

    private static boolean isValidMove(int N, boolean[][] bombBoard, boolean[][] visited, int row, int col) {
        return row >= 0 && row < N && col >= 0 && col < N && !bombBoard[row][col] && !visited[row][col];
    }

    private static void trackMoves(int[][] board, List<Position> path, Set<Position> bombPositions, Position castle) {
        for (int i = 1; i < path.size(); i++) {
            Position current = path.get(i);
            System.out.println("Move " + i + " --> Jump to " + board[current.row][current.col]);
            printBoardWithKnightCastleAndBombs(board, bombPositions, current, castle);
        }
    }
}