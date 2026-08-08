package com.replayrec.recording;

import com.replayrec.ReplayRecMod;
import com.replayrec.config.ModConfig;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VideoEncoder {
    private FFmpegFrameRecorder recorder;
    private Java2DFrameConverter converter;
    private boolean isRecording;
    private final ModConfig config;

    public VideoEncoder() {
        this.config = ModConfig.getInstance();
        this.converter = new Java2DFrameConverter();
    }

    public synchronized void start(String filename) throws IOException {
        if (isRecording) return;

        Path outputDir = Path.of(config.getOutputDir());
        Files.createDirectories(outputDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fullPath = outputDir.resolve(filename + "_" + timestamp + "." + config.getContainerFormat()).toString();

        recorder = new FFmpegFrameRecorder(fullPath, config.getWidth(), config.getHeight());
        recorder.setFormat(config.getContainerFormat());
        recorder.setVideoCodec(getVideoCodec());
        recorder.setVideoBitrate(config.getBitrate() * 1000);
        recorder.setFrameRate(config.getFps());
        recorder.setVideoOption("preset", getEncoderPreset());
        recorder.setVideoOption("tune", "zerolatency");

        if (config.isCaptureGameAudio()) {
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            recorder.setSampleRate(config.getAudioSampleRate());
            recorder.setAudioBitrate(config.getAudioBitrate());
            recorder.setAudioChannels(2);
        }

        recorder.setVideoOption("thread_queue", "1024");
        if (config.isCaptureGameAudio()) {
            recorder.setAudioOption("thread_queue", "1024");
        }

        recorder.start();
        isRecording = true;
        ReplayRecMod.LOGGER.info("Recording started: {}", fullPath);
    }

    public synchronized void encodeFrame(BufferedImage frame) {
        if (!isRecording || recorder == null) return;
        try {
            recorder.record(converter.convert(frame));
        } catch (Exception e) {
            ReplayRecMod.LOGGER.error("Failed to encode frame", e);
        }
    }

    public synchronized void encodeAudioSamples(short[] samples, int sampleRate, int channels) {
        if (!isRecording || recorder == null || !config.isCaptureGameAudio()) return;
        try {
            ShortBuffer buffer = ShortBuffer.wrap(samples);
            recorder.recordSamples(sampleRate, channels, buffer);
        } catch (Exception e) {
            ReplayRecMod.LOGGER.error("Failed to encode audio", e);
        }
    }

    public synchronized void stop() {
        if (!isRecording || recorder == null) return;
        try {
            recorder.stop();
            recorder.release();
            ReplayRecMod.LOGGER.info("Recording stopped and saved");
        } catch (Exception e) {
            ReplayRecMod.LOGGER.error("Failed to stop recorder", e);
        } finally {
            recorder = null;
            isRecording = false;
        }
    }

    private int getVideoCodec() {
        String encoder = config.getEncoder().toLowerCase();
        return switch (encoder) {
            case "h264_nvenc" -> avcodec.AV_CODEC_ID_H264;
            case "h265_nvenc" -> avcodec.AV_CODEC_ID_HEVC;
            default -> avcodec.AV_CODEC_ID_H264;
        };
    }

    private String getEncoderPreset() {
        String encoder = config.getEncoder().toLowerCase();
        return switch (encoder) {
            case "h264_nvenc", "h265_nvenc" -> "p1";
            default -> "ultrafast";
        };
    }

    public boolean isRecording() { return isRecording; }
}
