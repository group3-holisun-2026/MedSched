import React from 'react';

const Button = ({ children, variant = 'primary', className = '', ...props }) => {
  // Clasele de bază valabile pentru orice buton (padding, font, rotunjire)
  const baseStyles = "inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50 disabled:pointer-events-none px-4 py-2";
  
  // Variantele de culori mapate direct pe theme.css-ul tău
  const variants = {
    primary: "bg-primary text-primary-foreground hover:opacity-90",
    secondary: "bg-secondary text-secondary-foreground hover:opacity-80",
    destructive: "bg-destructive text-destructive-foreground hover:opacity-90",
    outline: "border border-input bg-transparent hover:bg-accent hover:text-accent-foreground"
  };

  return (
    <button 
      className={`${baseStyles} ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
};

export default Button;