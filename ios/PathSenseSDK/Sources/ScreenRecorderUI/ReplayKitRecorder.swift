import ReplayKit
import AVFoundation
import ScreenRecorderCore

public final class ReplayKitRecorder {
    private let recorder = RPScreenRecorder.shared()
    private var assetWriter: AVAssetWriter?
    private var videoInput: AVAssetWriterInput?
    private var audioInput: AVAssetWriterInput?
    private var outputURL: URL?
    private var startTime: CMTime?
    private var config: ScreenRecorderConfig?

    public func prepare(config: ScreenRecorderConfig) {
        self.config = config
        guard recorder.isAvailable else {
            // Report failure through KMM bridge
            return
        }

        let dir = FileManager.default.temporaryDirectory.appendingPathComponent("screen-recorder")
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)

        let ext = config.outputFormat == .mp4 ? "mp4" : "mov"
        outputURL = dir.appendingPathComponent("rec_\(Date().timeIntervalSince1970).\(ext)")

        guard let url = outputURL else { return }

        do {
            let fileType: AVFileType = config.outputFormat == .mp4 ? .mp4 : .mov
            assetWriter = try AVAssetWriter(outputURL: url, fileType: fileType)

            let videoSettings: [String: Any] = [
                AVVideoCodecKey: AVVideoCodecType.h264,
                AVVideoWidthKey: config.videoQuality.width,
                AVVideoHeightKey: config.videoQuality.height,
                AVVideoCompressionPropertiesKey: [
                    AVVideoAverageBitRateKey: config.videoQuality.bitrateMbps * 1_000_000,
                ]
            ]
            videoInput = AVAssetWriterInput(mediaType: .video, outputSettings: videoSettings)
            videoInput?.expectsMediaDataInRealTime = true
            assetWriter?.add(videoInput!)

            if config.audioEnabled {
                let audioSettings: [String: Any] = [
                    AVFormatIDKey: kAudioFormatMPEG4AAC,
                    AVSampleRateKey: 44100,
                    AVNumberOfChannelsKey: 2,
                ]
                audioInput = AVAssetWriterInput(mediaType: .audio, outputSettings: audioSettings)
                audioInput?.expectsMediaDataInRealTime = true
                assetWriter?.add(audioInput!)
            }
        } catch {
            return
        }
    }

    public func startCapture() {
        assetWriter?.startWriting()
        startTime = nil

        recorder.startCapture { [weak self] sampleBuffer, bufferType, error in
            guard let self = self, error == nil else { return }

            if self.startTime == nil {
                let time = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
                self.startTime = time
                self.assetWriter?.startSession(atSourceTime: time)
            }

            switch bufferType {
            case .video:
                if self.videoInput?.isReadyForMoreMediaData == true {
                    self.videoInput?.append(sampleBuffer)
                }
            case .audioMic:
                if self.audioInput?.isReadyForMoreMediaData == true {
                    self.audioInput?.append(sampleBuffer)
                }
            default:
                break
            }
        } completionHandler: { error in
            if let error = error {
                print("ScreenRecorder: startCapture failed: \(error)")
            }
        }
    }

    public func stopCapture(completion: @escaping (URL?, Int64) -> Void) {
        recorder.stopCapture { [weak self] error in
            guard let self = self else {
                completion(nil, 0)
                return
            }

            self.videoInput?.markAsFinished()
            self.audioInput?.markAsFinished()

            self.assetWriter?.finishWriting {
                guard self.assetWriter?.status == .completed,
                      let url = self.outputURL else {
                    completion(nil, 0)
                    return
                }

                let attrs = try? FileManager.default.attributesOfItem(atPath: url.path)
                let size = (attrs?[.size] as? Int64) ?? 0

                // Get duration from the asset
                let asset = AVURLAsset(url: url)
                let durationMs = Int64(CMTimeGetSeconds(asset.duration) * 1000)

                completion(url, durationMs)
            }
        }
    }

    public func release() {
        assetWriter = nil
        videoInput = nil
        audioInput = nil
        outputURL = nil
        startTime = nil
    }
}
