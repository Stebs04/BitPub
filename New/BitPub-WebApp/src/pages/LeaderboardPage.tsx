import React from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import { Trophy } from 'lucide-react';

const LeaderboardPage: React.FC = () => {
  return (
    <div className="p-8 animate-slide-up">
      <div className="flex items-center gap-3 mb-8">
        <Trophy className="w-8 h-8 text-brand-light" />
        <h1 className="text-3xl font-bold text-white">Leaderboard Globale</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Top Giocatori (Biliardo)</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 text-slate-400">
                  <th className="py-4 px-4 font-medium">Pos</th>
                  <th className="py-4 px-4 font-medium">Giocatore</th>
                  <th className="py-4 px-4 font-medium">Punteggio</th>
                  <th className="py-4 px-4 font-medium">Vittorie</th>
                </tr>
              </thead>
              <tbody className="text-slate-200">
                <tr className="border-b border-white/5 hover:bg-white/5 transition-colors">
                  <td className="py-4 px-4 text-brand-light font-bold">1</td>
                  <td className="py-4 px-4">Mario Rossi</td>
                  <td className="py-4 px-4 font-semibold text-emerald-400">12,450</td>
                  <td className="py-4 px-4">145</td>
                </tr>
                <tr className="border-b border-white/5 hover:bg-white/5 transition-colors">
                  <td className="py-4 px-4 text-slate-300 font-bold">2</td>
                  <td className="py-4 px-4">Luigi Verdi</td>
                  <td className="py-4 px-4 font-semibold text-emerald-400">11,200</td>
                  <td className="py-4 px-4">132</td>
                </tr>
                <tr className="border-b border-white/5 hover:bg-white/5 transition-colors">
                  <td className="py-4 px-4 text-orange-400 font-bold">3</td>
                  <td className="py-4 px-4">Anna Bianchi</td>
                  <td className="py-4 px-4 font-semibold text-emerald-400">10,850</td>
                  <td className="py-4 px-4">110</td>
                </tr>
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default LeaderboardPage;
