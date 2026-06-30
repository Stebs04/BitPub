import React from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import { MapPin, Server } from 'lucide-react';

const LocalesPage: React.FC = () => {
  return (
    <div className="p-8 animate-slide-up">
      <div className="flex items-center gap-3 mb-8">
        <MapPin className="w-8 h-8 text-brand-light" />
        <h1 className="text-3xl font-bold text-white">Gestione Locali</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle>BitPub Milano Centrale</CardTitle>
              <span className="px-3 py-1 bg-emerald-500/20 text-emerald-400 rounded-full text-xs font-semibold">
                ONLINE
              </span>
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-slate-400 mb-4">Via Roma 10, Milano (MI)</p>
            <div className="space-y-3">
              <div className="flex items-center gap-3 text-slate-300 bg-white/5 p-3 rounded-lg">
                <Server className="w-5 h-5 text-brand" />
                <span>Edge Node: <strong>Node-1A</strong></span>
              </div>
              <div className="flex justify-between text-sm text-slate-400 px-1">
                <span>Macchine installate:</span>
                <span className="text-white font-semibold">3 (Biliardo, Freccette)</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default LocalesPage;
