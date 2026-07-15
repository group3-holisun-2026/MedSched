import React from 'react';
import Button from './Button'; 

const Modal = ({ isOpen, onClose, title, children }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm">
      <div className="w-full max-w-lg rounded-lg border border-border bg-popover p-6 text-popover-foreground shadow-lg">
        {/* Titlul Modalului */}
        {title && <h2 className="mb-4 text-xl font-medium">{title}</h2>}
        
        {/* Conținutul (formular, text de confirmare, etc) */}
        <div className="mb-6">
          {children}
        </div>
        
        {/* Butonul de închidere */}
        <div className="flex justify-end space-x-2">
          <Button variant="outline" onClick={onClose}>
            Anulează
          </Button>
        </div>
      </div>
    </div>
  );
};

export default Modal;