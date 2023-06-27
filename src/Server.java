import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final int PORT = 1234;
    private static  String USERNAME = "Hamza Javaid";
    private static final String PASSWORD = "0767";
    private static final int QUESTION_TIMEOUT_SECONDS = 15;

    private static final List<Question> generalKnowledgeQuestions = new ArrayList<>();

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
            int score = 0;

            for (int i = 0; i < generalKnowledgeQuestions.size(); i++) {
                Question question = generalKnowledgeQuestions.get(i);
                writer.println("Question " + (i + 1) + ": " + question.getQuestionText());
                writer.println("Options:");

                List<String> options = question.getOptions();
                for (int j = 0; j < options.size(); j++) {
                    writer.println((char) (j) + ") " + options.get(j));
                }

                QuizTimerTask timerTask = new QuizTimerTask(reader, writer, quiz, question);
                Timer timer = new Timer();
                timer.schedule(timerTask, QUESTION_TIMEOUT_SECONDS * 1000);

                String answer = timerTask.waitForAnswer();

                timer.cancel();

                quiz.addAnswer(question.getQuestionText(), answer);

                String correctAnswer = question.getCorrectAnswer();
                if (answer.equalsIgnoreCase(correctAnswer)) {
                    writer.println("Your answer is correct!");
                    score += 1;
                } else {
                    writer.println("Your answer is incorrect. The correct answer is: " + correctAnswer);
                }
            }

            // Store the quiz in a separate file with the user's name
            String fileName = USERNAME.replace(" ", "_") + "_quiz_answers.txt";
            storeQuiz(quiz, fileName);

            // Calculate percentage
            int totalMarks = generalKnowledgeQuestions.size();
            int obtainedMarks = score;
            double percentage = (obtainedMarks * 100.0) / totalMarks;

            // Send score and percentage to the user
            writer.println("Quiz completed. Your answers have been saved.");
            writer.println("Your Score: " + obtainedMarks + "/" + totalMarks);
            writer.println("Percentage: " + percentage + "%");

            // Close the connection
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean performAuthentication(BufferedReader reader, PrintWriter writer) throws IOException {
        writer.println("Please enter your username:");
        USERNAME = reader.readLine();
        writer.println("Please enter your password:");
        String password = reader.readLine();

        return  password.equals(PASSWORD);
    }

    private static void storeQuiz(Quiz quiz, String fileName) {
        String fileNamee = USERNAME + "_quiz_answers.txt";
        try (PrintWriter writer = new PrintWriter(fileNamee)) {
            writer.println(quiz.toString());
            System.out.println("Quiz answers saved to " + fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void initializeGeneralKnowledgeQuestions() {
        Question question1 = new Question(
                "Which planet is known as the Red Planet?",
                Arrays.asList("A) Venus", "B) Mars", "C) Jupiter", "D) Saturn"),
                "B"
        );
        Question question2 = new Question(
                "What is the capital of France?",
                Arrays.asList("A) London", "B) Paris", "C) Rome", "D) Madrid"),
                "B"
        );
        Question question3 = new Question(
                "Who painted the Mona Lisa?",
                Arrays.asList("A) Leonardo da Vinci", "B) Vincent van Gogh", "C) Pablo Picasso", "D) Michelangelo"),
                "A"
        );
        Question question4 = new Question(
                "Which country won the FIFA World Cup in 2018?",
                Arrays.asList("A) Germany", "B) Brazil", "C) France", "D) Argentina"),
                "C"
        );
        Question question5 = new Question(
                "What is the largest ocean on Earth?",
                Arrays.asList("A) Atlantic Ocean", "B) Indian Ocean", "C) Arctic Ocean", "D) Pacific Ocean"),
                "D"
        );

        generalKnowledgeQuestions.add(question1);
        generalKnowledgeQuestions.add(question2);
        generalKnowledgeQuestions.add(question3);
        generalKnowledgeQuestions.add(question4);
        generalKnowledgeQuestions.add(question5);
    }

    private static class Question {
        private final String questionText;
        private final List<String> options;
        private final String correctAnswer;

        public Question(String questionText, List<String> options, String correctAnswer) {
            this.questionText = questionText;
            this.options = options;
            this.correctAnswer = correctAnswer;
        }

        public String getQuestionText() {
            return questionText;
        }

        public List<String> getOptions() {
            return options;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }
    }

    private static class Quiz {
        private final Map<String, String> questionAnswers;

        public Quiz() {
            questionAnswers = new HashMap<>();
        }

        public void addAnswer(String question, String answer) {
            questionAnswers.put(question, answer);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : questionAnswers.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            return sb.toString();
        }
    }

    private static class QuizTimerTask extends TimerTask {
        private final BufferedReader reader;
        private final PrintWriter writer;
        private final Quiz quiz;
        private final Question question;

        public QuizTimerTask(BufferedReader reader, PrintWriter writer, Quiz quiz, Question question) {
            this.reader = reader;
            this.writer = writer;
            this.quiz = quiz;
            this.question = question;
        }

        @Override
        public void run() {
            try {
                if (!reader.ready()) {
                    writer.println("Time's up! Moving to the next question.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String waitForAnswer() {
            try {
                long startTime = System.currentTimeMillis();
                while (!reader.ready()) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - startTime;
                    long remainingTime = TimeUnit.SECONDS.toMillis(QUESTION_TIMEOUT_SECONDS) - elapsedTime;

                    if (remainingTime <= 0) {
                        return "";
                    }
                }
                return reader.readLine().trim();
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }
    }
}
