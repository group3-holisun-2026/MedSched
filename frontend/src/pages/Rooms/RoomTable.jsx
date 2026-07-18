import React from 'react';
const RoomTable = ({ rooms, onEdit }) => {
  if (!rooms || rooms.length === 0) {
    return (
      <div className="text-center py-10 bg-white rounded-xl border border-gray-100 shadow-sm mt-4">
        <p className="text-gray-500">Nu a fost găsit niciun cabinet.</p>
      </div>
    );
  }
  const getInitials = (name) => {
    if (!name) return "C";
    const words = name.split(" ").filter(Boolean);
    if (words.length >= 2) {
      return (words[0][0] + words[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };
  const colors = ["#6366f1", "#f59e0b", "#10b981", "#f43f5e", "#8b5cf6", "#0ea5e9"];
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
      {rooms.map((room, index) => {
        const color = colors[index % colors.length];
        
        return (
          <div key={room.id} className="bg-white rounded-xl p-5 border border-gray-100 shadow-sm flex items-start gap-4 hover:shadow-md transition-shadow relative group">
            <div 
              className="w-14 h-14 rounded-xl flex items-center justify-center text-white font-bold text-lg flex-shrink-0"
              style={{ backgroundColor: color }}
            >
              {getInitials(room.name)}
            </div>
            
            <div className="flex-1 min-w-0 pr-8">
              <h3 className="font-semibold text-gray-900 truncate">{room.name}</h3>
              <p className="text-sm text-gray-500 line-clamp-2 mt-1">{room.description || "Fără descriere"}</p>
              
              <div className="mt-3 flex items-center gap-2">
                <span className={`text-xs px-2 py-1 rounded-full ${room.active ? 'bg-emerald-50 text-emerald-600' : 'bg-gray-100 text-gray-600'}`}>
                  {room.active ? 'Activ' : 'Inactiv'}
                </span>
              </div>
            </div>
            <button 
              onClick={() => onEdit(room)}
              className="absolute top-4 right-4 p-2 rounded-lg text-gray-400 hover:text-blue-600 hover:bg-blue-50 transition-colors"
              title="Editează cabinet"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path>
              </svg>
            </button>
            
          </div>
        );
      })}
    </div>
  );
};
export default RoomTable;
