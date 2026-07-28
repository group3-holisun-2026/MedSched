import React from 'react';
import Button from '../Button';

export default function ExportButtons({ onExportCSV, onExportPDF }) {
  return (
    <div className="flex gap-2">
      <Button type="button" variant="outline" onClick={onExportCSV}>
        Export CSV
      </Button>
      <Button type="button" variant="outline" onClick={onExportPDF}>
        Export PDF
      </Button>
    </div>
  );
}