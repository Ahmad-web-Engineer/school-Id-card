import React, { useState } from 'react';
import { useAuthStore } from '../../store/useAuthStore';

function getInitials(name = '') {
  if (!name || typeof name !== 'string') return '?';
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

const sizeClasses = {
  sm: 'w-7 h-7 text-[10px]',
  md: 'w-9 h-9 text-xs',
  lg: 'w-16 h-16 text-sm',
  xl: 'w-24 h-24 text-xl',
};

export default function StudentPhotoAvatar({
  photoFileId,
  name = '',
  size = 'md',
  className = '',
  alt,
}) {
  const token = useAuthStore((state) => state.token);
  const [hasError, setHasError] = useState(false);
  const sizeClass = sizeClasses[size] || sizeClasses.md;
  const initials = getInitials(name);

  // If there's a valid photo ID and no error loading it yet
  if (photoFileId && !hasError) {
    const photoUrl = `/api/v1/files/${photoFileId}${token ? `?token=${encodeURIComponent(token)}` : ''}`;
    return (
      <img
        src={photoUrl}
        alt={alt || `${name || 'Student'}'s portrait`}
        onError={() => setHasError(true)}
        className={`${sizeClass} rounded-full object-cover border border-slate-200 shadow-xs shrink-0 ${className}`}
      />
    );
  }

  // Fallback: Initials avatar with pleasant brand colors
  return (
    <div
      aria-label={alt || `${name || 'Student'}'s initials`}
      className={`${sizeClass} rounded-full bg-indigo-50 border border-indigo-200/80 text-indigo-700 font-bold flex items-center justify-center select-none shrink-0 ${className}`}
    >
      {initials}
    </div>
  );
}
