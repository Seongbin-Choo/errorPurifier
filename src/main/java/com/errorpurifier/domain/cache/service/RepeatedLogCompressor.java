package com.errorpurifier.domain.cache.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class RepeatedLogCompressor {

    private static final int MINIMUM_REPEAT_BLOCKS = 4;
    private static final Pattern TIMESTAMP_START = Pattern.compile("^\\s*(?:\\[?\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?\\]?|\\d{2}:\\d{2}:\\d{2}[.,]\\d+)");
    private static final Pattern LOG_LEVEL_START = Pattern.compile("^\\s*(?:\\[[^]]+])?\\s*(?:TRACE|DEBUG|INFO|WARN|ERROR)\\b");
    private static final Pattern EXCEPTION_START = Pattern.compile("^\\s*(?:(?:Caused by|Suppressed):\\s+)?(?:[\\w$]+\\.)*[\\w$]+(?:Exception|Error)(?::|\\s|$)|^\\s*Exception in thread ");
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\\u001B\\[[;\\d]*m");
    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}");
    private static final Pattern NUMBER = Pattern.compile("(?<![A-Za-z])\\d+(?:[.,]\\d+)?(?![A-Za-z])");
    private static final Pattern NUMBER_WITH_UNIT = Pattern.compile("\\d+(?:[.,]\\d+)?(?=[A-Za-z]+\\b)");

    public CompressionResult compress(String text) {
        List<String> blocks = splitIntoLogBlocks(text);
        if (blocks.size() < MINIMUM_REPEAT_BLOCKS) {
            return new CompressionResult(text, 0, 0, 0);
        }

        List<String> compressed = new ArrayList<>();
        int repeatedBlockCount = 0;
        int omittedBlockCount = 0;
        for (int index = 0; index < blocks.size();) {
            int endExclusive = index + 1;
            String signature = signatureOf(blocks.get(index));
            while (endExclusive < blocks.size() && signature.equals(signatureOf(blocks.get(endExclusive)))) {
                endExclusive++;
            }
            int runLength = endExclusive - index;
            if (runLength >= MINIMUM_REPEAT_BLOCKS) {
                compressed.add(blocks.get(index));
                compressed.add(blocks.get(index + 1));
                int omitted = runLength - 3;
                compressed.add("[... 유사한 반복 로그 블록 " + runLength + "회 중 " + omitted
                        + "회 생략: 첫 2개와 마지막 1개 보존 ...]");
                compressed.add(blocks.get(endExclusive - 1));
                repeatedBlockCount += runLength;
                omittedBlockCount += omitted;
            } else {
                compressed.addAll(blocks.subList(index, endExclusive));
            }
            index = endExclusive;
        }

        String compressedText = String.join("\n", compressed);
        return new CompressionResult(compressedText, repeatedBlockCount, omittedBlockCount,
                Math.max(0, text.length() - compressedText.length()));
    }

    private List<String> splitIntoLogBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        StringBuilder currentBlock = new StringBuilder();
        String[] lines = text.split("\\n", -1);
        boolean hasStructuredLogStarts = java.util.Arrays.stream(lines).anyMatch(this::isStructuredLogBlockStart);
        for (String line : lines) {
            boolean startsNewBlock = hasStructuredLogStarts ? isStructuredLogBlockStart(line) : EXCEPTION_START.matcher(line).find();
            if (currentBlock.length() > 0 && startsNewBlock) {
                blocks.add(currentBlock.toString());
                currentBlock.setLength(0);
            }
            if (currentBlock.length() > 0) {
                currentBlock.append('\n');
            }
            currentBlock.append(line);
        }
        if (currentBlock.length() > 0) {
            blocks.add(currentBlock.toString());
        }
        return blocks;
    }

    private boolean isStructuredLogBlockStart(String line) {
        return TIMESTAMP_START.matcher(line).find() || LOG_LEVEL_START.matcher(line).find();
    }

    private String signatureOf(String block) {
        String normalized = ANSI_ESCAPE.matcher(block).replaceAll("");
        normalized = UUID.matcher(normalized).replaceAll("<uuid>");
        normalized = NUMBER_WITH_UNIT.matcher(normalized).replaceAll("#");
        normalized = NUMBER.matcher(normalized).replaceAll("#");
        return normalized.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    public record CompressionResult(String text, int repeatedBlockCount, int omittedBlockCount, int savedCharacters) {
    }
}
