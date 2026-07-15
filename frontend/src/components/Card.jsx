import React from 'react';

const Card = ({ children, className = '' }) => {
  return (
    <div className={`rounded-xl border border-border bg-card text-card-foreground shadow-sm p-6 ${className}`}>
      {children}
    </div>
  );
};

export default Card;