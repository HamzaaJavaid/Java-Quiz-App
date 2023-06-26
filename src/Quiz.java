

import java.util.HashMap;
import java.util.Map;

public class Quiz {
    private Map<String, String> questionAnswers;

    public Quiz() {
        questionAnswers = new HashMap<>();
    }

    public void addQuestionAnswer(String question, String answer) {
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
