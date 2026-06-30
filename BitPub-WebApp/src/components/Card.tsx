import React from 'react';
import { cn } from './Button';

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
}

export const Card: React.FC<CardProps> = ({ className, children, ...props }) => {
  return (
    <div 
      className={cn("glass-panel p-6 animate-fade-in", className)} 
      {...props}
    >
      {children}
    </div>
  );
};

export const CardHeader: React.FC<CardProps> = ({ className, children, ...props }) => {
  return (
    <div className={cn("mb-4", className)} {...props}>
      {children}
    </div>
  );
};

export const CardTitle: React.FC<CardProps> = ({ className, children, ...props }) => {
  return (
    <h3 className={cn("text-xl font-semibold text-white", className)} {...props}>
      {children}
    </h3>
  );
};

export const CardContent: React.FC<CardProps> = ({ className, children, ...props }) => {
  return (
    <div className={className} {...props}>
      {children}
    </div>
  );
};
