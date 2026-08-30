import React, { useState, useEffect } from 'react';
import { localInference, LocalInferenceEngine } from '../services/localInferenceEngine';
import { ModelManager, AVAILABLE_MODELS } from '../services/modelManager';

/**
 * UI Component displaying online/offline network status, WebGPU support, and model caching.
 */
export default function OfflineModeIndicator({ onToggleOffline, isOfflineOnly }) {
    const [isOnline, setIsOnline] = useState(navigator.onLine);
    const [hardware, setHardware] = useState({ hasWebGPU: false, adapterInfo: 'Checking...' });
    const [selectedModel, setSelectedModel] = useState('smollm2-360m');
    const [isDownloading, setIsDownloading] = useState(false);
    const [downloadPercent, setDownloadPercent] = useState(0);
    const [isModelReady, setIsModelReady] = useState(false);

    useEffect(() => {
        const handleOnline = () => setIsOnline(true);
        const handleOffline = () => setIsOnline(false);

        window.addEventListener('online', handleOnline);
        window.addEventListener('offline', handleOffline);

        LocalInferenceEngine.checkHardwareSupport().then(setHardware);
        ModelManager.isModelCached(selectedModel).then(setIsModelReady);

        return () => {
            window.removeEventListener('online', handleOnline);
            window.removeEventListener('offline', handleOffline);
        };
    }, [selectedModel]);

    const handleDownloadModel = async () => {
        setIsDownloading(true);
        setDownloadPercent(0);

        try {
            await ModelManager.downloadAndCacheModel(selectedModel, ({ percent }) => {
                setDownloadPercent(percent);
            });
            await localInference.init(selectedModel);
            setIsModelReady(true);
        } catch (err) {
            console.error('Failed to download offline model:', err);
        } finally {
            setIsDownloading(false);
        }
    };

    return (
        <div className="p-4 bg-white dark:bg-gray-800 rounded-xl shadow border border-gray-200 dark:border-gray-700 text-sm mb-4">
            <div className="flex items-center justify-between flex-wrap gap-2">
                <div className="flex items-center gap-2">
                    <span className={`h-3 w-3 rounded-full ${isOnline ? 'bg-green-500' : 'bg-amber-500 animate-pulse'}`} />
                    <span className="font-bold text-gray-900 dark:text-white">
                        {isOnline ? 'Online (Cloud AI)' : 'Offline (On-Device WebGPU Mode)'}
                    </span>
                    <span className="text-xs px-2 py-0.5 bg-gray-100 dark:bg-gray-700 rounded text-gray-600 dark:text-gray-300">
                        {hardware.adapterInfo}
                    </span>
                </div>

                <div className="flex items-center gap-2">
                    <select
                        value={selectedModel}
                        onChange={(e) => setSelectedModel(e.target.value)}
                        disabled={isDownloading}
                        className="px-2 py-1 border rounded dark:bg-gray-700 dark:text-white text-xs"
                    >
                        {AVAILABLE_MODELS.map(m => (
                            <option key={m.id} value={m.id}>{m.name} ({m.sizeMb}MB)</option>
                        ))}
                    </select>

                    {!isModelReady ? (
                        <button
                            onClick={handleDownloadModel}
                            disabled={isDownloading}
                            className="px-3 py-1 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded text-xs transition"
                        >
                            {isDownloading ? `Downloading (${downloadPercent}%)` : 'Download for Offline'}
                        </button>
                    ) : (
                        <span className="text-xs text-green-600 dark:text-green-400 font-semibold flex items-center gap-1">
                            ✓ Ready Offline
                        </span>
                    )}
                </div>
            </div>

            {isDownloading && (
                <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-1.5 mt-3">
                    <div
                        className="bg-indigo-600 h-1.5 rounded-full transition-all duration-200"
                        style={{ width: `${downloadPercent}%` }}
                    />
                </div>
            )}
        </div>
    );
}
