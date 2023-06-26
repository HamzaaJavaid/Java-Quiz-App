import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final int PORT = 1234;
    private static final String USERNAME = "Hamza Javaid";
    private static final String PASSWORD = "0767";
    private static final int QUESTION_TIMEOUT_SECONDS = 15;

    private static final List<String> generalKnowledgeQuestions = new ArrayList<>();

    public static void main(String[] args) {
        initializeGeneralKnowledgeQuestions();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started. Listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());

                Thread clientThread = new Thread(() -> handleClient(socket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Send initial greeting
            writer.println("Welcome to the Quiz Game!");

            // Perform authentication
            boolean isAuthenticated = performAuthentication(reader, writer);
            if (!isAuthenticated) {
                writer.println("Authentication failed. Closing connection.");
                socket.close();
                return;
            }

            // Start the quiz
            Quiz quiz = new Quiz();
            for (int i = 0; i < generalKnowledgeQuestions.size(); i++) {
                String question = generalKnowledgeQuestions.get(i);
                writer.println("Question " + (i + 1) + ": " + question);

                QuizTimerTask timerTask = new QuizTimerTask(reader, quiz, question);
                Timer timer = new Timer();
                timer.schedule(timerTask, TimeUnit.SECONDS.toMillis(QUESTION_TIMEOUT_SECONDS));

                String answer = timerTask.waitForAnswer();

                timer.cancel();

                quiz.addQuestionAnswer(question, answer);
            }

            // Store the quiz in a separate text file
            storeQuiz(quiz);

            // Send completion message
            writer.println("Quiz completed. Your answers have been saved. Goodbye!");

            // Close the connection
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean performAuthentication(BufferedReader reader, PrintWriter writer) throws IOException {
        writer.println("Please enter your username:");
        String username = reader.readLine();

        writer.println("Please enter your password:");
        String password = reader.readLine();

        return username.equals(USERNAME) && password.equals(PASSWORD);
    }

    private static void storeQuiz(Quiz quiz) {
        try (PrintWriter writer = new PrintWriter("quiz_answers.txt")) {
            writer.println(quiz.toString());
            System.out.println("Quiz answers saved to quiz_answers.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void initializeGeneralKnowledgeQuestions() {
        generalKnowledgeQuestions.add("Who invented the telephone?");
        generalKnowledgeQuestions.add("What is the capital of France?");
        generalKnowledgeQuestions.add("Who painted the Mona Lisa?");
        generalKnowledgeQuestions.add("What is the largest planet in our solar system?");
        generalKnowledgeQuestions.add("What year was the first manned moon landing?");
    }

    private static class QuizTimerTask extends TimerTask {
        private final BufferedReader reader;
        private final Quiz quiz;
        private final String question;
        private String answer;

        public QuizTimerTask(BufferedReader reader, Quiz quiz, String question) {
            this.reader = reader;
            this.quiz = quiz;
            this.question = question;
        }

        @Override
        public void run() {
            try {
                if (answer == null) {
                    answer = ""; // Set blank answer if no response received within the timeout
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String waitForAnswer() {
            try {
                long startTime = System.currentTimeMillis();
                while ((System.currentTimeMillis() - startTime) < TimeUnit.SECONDS.toMillis(QUESTION_TIMEOUT_SECONDS)) {
                    if (reader.ready()) {
                        answer = reader.readLine();
                        return answer;
                    }
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return "";
        }
    }
}



