import React, { useRef, useState, useEffect } from 'react';
import { Camera, Upload, X, AlertCircle } from 'lucide-react';
import StudentPhotoAvatar from './StudentPhotoAvatar';
import Button from '../../components/common/Button';

export default function SinglePhotoUpload({
  currentPhotoFileId = null,
  studentName = '',
  selectedFile = null,
  onFileSelect,
  onFileRemove,
  disabled = false,
}) {
  const fileInputRef = useRef(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [fileError, setFileError] = useState('');

  // Handle local preview for newly chosen file
  useEffect(() => {
    if (!selectedFile) {
      setPreviewUrl(null);
      return;
    }

    const objectUrl = URL.createObjectURL(selectedFile);
    setPreviewUrl(objectUrl);

    return () => {
      URL.revokeObjectURL(objectUrl);
    };
  }, [selectedFile]);

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    setFileError('');
    if (!file) return;

    // Validate type
    if (!['image/jpeg', 'image/jpg', 'image/png'].includes(file.type.toLowerCase())) {
      setFileError('Only JPG, JPEG and PNG formats are supported.');
      return;
    }

    // Validate size (5MB max)
    const MAX_SIZE = 5 * 1024 * 1024;
    if (file.size > MAX_SIZE) {
      setFileError('Photo exceeds maximum allowed size of 5 MB.');
      return;
    }

    onFileSelect(file);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleRemove = () => {
    setFileError('');
    onFileRemove();
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="p-3.5 bg-slate-50/70 border border-slate-200 rounded-lg">
      <div className="flex items-start gap-4">
        {/* Preview Container */}
        <div className="relative shrink-0">
          {previewUrl ? (
            <div className="relative">
              <img
                src={previewUrl}
                alt="Selected student preview"
                className="w-16 h-20 sm:w-20 sm:h-24 rounded-md object-cover border-2 border-indigo-500 shadow-sm"
              />
              <span className="absolute -top-1.5 -right-1.5 bg-indigo-600 text-white rounded-full p-0.5 shadow-xs">
                <Camera className="w-3 h-3" />
              </span>
            </div>
          ) : currentPhotoFileId ? (
            <div className="flex flex-col items-center">
              <img
                src={`/api/v1/files/${currentPhotoFileId}`}
                alt={`${studentName || 'Student'} photo`}
                className="w-16 h-20 sm:w-20 sm:h-24 rounded-md object-cover border border-slate-300 shadow-xs"
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                  e.currentTarget.nextSibling.style.display = 'flex';
                }}
              />
              <div
                style={{ display: 'none' }}
                className="w-16 h-20 sm:w-20 sm:h-24 rounded-md bg-slate-200 border border-slate-300 items-center justify-center text-xs font-bold text-slate-600"
              >
                Avatar
              </div>
            </div>
          ) : (
            <div className="w-16 h-20 sm:w-20 sm:h-24 rounded-md border-2 border-dashed border-slate-300 bg-white flex flex-col items-center justify-center text-slate-400">
              <Camera className="w-6 h-6 stroke-1 mb-1" />
              <span className="text-[10px] font-medium text-slate-500">No Photo</span>
            </div>
          )}
        </div>

        {/* Upload Controls & Details */}
        <div className="flex-1 min-w-0">
          <label className="block text-xs font-semibold text-slate-800 mb-1">
            Student Photo (Optional)
          </label>
          <p className="text-[11px] text-slate-500 mb-2 leading-relaxed">
            Upload an ID card portrait (.jpg or .png, up to 5 MB). The photo will be printed directly on the ID card.
          </p>

          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/jpg,image/png"
            disabled={disabled}
            onChange={handleFileChange}
            className="hidden"
            id="student-photo-file-input"
            aria-label="Upload student portrait"
          />

          <div className="flex flex-wrap items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={disabled}
              onClick={() => fileInputRef.current?.click()}
            >
              <Upload className="w-3.5 h-3.5 mr-1 text-slate-600" />
              {selectedFile || currentPhotoFileId ? 'Replace Photo' : 'Choose Photo'}
            </Button>

            {selectedFile && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={disabled}
                onClick={handleRemove}
                className="text-rose-600 hover:text-rose-700 hover:bg-rose-50"
              >
                <X className="w-3.5 h-3.5 mr-1" />
                Remove
              </Button>
            )}

            {selectedFile && (
              <span className="text-[11px] font-medium text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200 truncate max-w-[180px]">
                {selectedFile.name}
              </span>
            )}
          </div>

          {fileError && (
            <div className="mt-2 text-xs text-rose-600 font-medium flex items-center gap-1.5">
              <AlertCircle className="w-3.5 h-3.5 shrink-0" />
              <span>{fileError}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
