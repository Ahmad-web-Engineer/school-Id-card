import React, { useState } from 'react';
import { Users, FileSpreadsheet, Camera } from 'lucide-react';
import StudentRosterTab from '../../features/students/StudentRosterTab';
import StudentImportTab from '../../features/students/StudentImportTab';
import PhotoMatcherTab from '../../features/students/PhotoMatcherTab';

export default function StudentsPage() {
  const [activeTab, setActiveTab] = useState('roster'); // 'roster', 'import', 'matcher'

  const tabs = [
    {
      id: 'roster',
      label: 'Student Roster',
      icon: Users,
      description: 'Directory, enrollment, and individual student profiles',
    },
    {
      id: 'import',
      label: 'Import Students',
      icon: FileSpreadsheet,
      description: 'Bulk Excel & CSV import wizard with error validation',
    },
    {
      id: 'matcher',
      label: 'Photo Matcher',
      icon: Camera,
      description: 'Bulk ZIP photo upload & auto-matching wizard',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div>
        <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
          Students & Photo Center
        </h1>
        <p className="text-xs sm:text-sm text-slate-500 mt-1 max-w-3xl">
          Maintain your school roster, add new admissions, import students via spreadsheet,
          and automatically match portrait photos for ID card production.
        </p>
      </div>

      {/* Top Switcher / Action Bar with Tabs */}
      <div className="border-b border-slate-200">
        <nav
          className="-mb-px flex space-x-2 sm:space-x-4 overflow-x-auto pb-1"
          role="tablist"
          aria-label="Student management tabs"
        >
          {tabs.map((tab) => {
            const isActive = activeTab === tab.id;
            const TabIcon = tab.icon;
            return (
              <button
                key={tab.id}
                role="tab"
                id={`tab-${tab.id}`}
                aria-selected={isActive}
                aria-controls={`panel-${tab.id}`}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-3.5 py-2.5 text-xs sm:text-sm font-semibold border-b-2 whitespace-nowrap transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 rounded-t-md ${
                  isActive
                    ? 'border-indigo-600 text-indigo-600 bg-indigo-50/40'
                    : 'border-transparent text-slate-500 hover:text-slate-800 hover:border-slate-300'
                }`}
              >
                <TabIcon
                  className={`h-4 w-4 ${
                    isActive ? 'text-indigo-600' : 'text-slate-400 group-hover:text-slate-600'
                  }`}
                />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </nav>
      </div>

      {/* Tab Panels */}
      <div className="mt-4">
        {activeTab === 'roster' && (
          <section
            id="panel-roster"
            role="tabpanel"
            aria-labelledby="tab-roster"
            tabIndex="0"
          >
            <StudentRosterTab />
          </section>
        )}

        {activeTab === 'import' && (
          <section
            id="panel-import"
            role="tabpanel"
            aria-labelledby="tab-import"
            tabIndex="0"
          >
            <StudentImportTab onNavigateToRoster={() => setActiveTab('roster')} />
          </section>
        )}

        {activeTab === 'matcher' && (
          <section
            id="panel-matcher"
            role="tabpanel"
            aria-labelledby="tab-matcher"
            tabIndex="0"
          >
            <PhotoMatcherTab onNavigateToRoster={() => setActiveTab('roster')} />
          </section>
        )}
      </div>
    </div>
  );
}
