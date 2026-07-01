"use client";

import { useState } from "react";
import { User, MessageCircle, MoreHorizontal } from "lucide-react";
import { cn } from "@/lib/utils";

const games = [
    { id: "khasina", title: "KHASINA", description: "Strategic card capturing game" },
    { id: "umlabalaba", title: "UMLABALABA", description: "Traditional board game" }
];

export default function Dashboard() {
    const [isProfileOpen, setIsProfileOpen] = useState(false);

    return (
        <div className="flex flex-col h-screen p-6 max-w-6xl mx-auto">
            {/* Top Bar */}
            <header className="flex justify-between items-center mb-12">
                <h1 className="text-3xl font-bold tracking-tight text-[#E0BC7A]">PLAYZULU</h1>
                <div className="flex gap-4">
                    <button className="p-2 rounded-full hover:bg-white/5">
                        <MessageCircle className="w-8 h-8 text-[#E0BC7A]" />
                    </button>
                    <button
                        onClick={() => setIsProfileOpen(true)}
                        className="p-2 rounded-full hover:bg-white/5"
                    >
                        <User className="w-8 h-8 text-[#E0BC7A]" />
                    </button>
                </div>
            </header>

            {/* Game Selection */}
            <main className="flex-1">
                <h2 className="text-xl font-medium text-white/70 mb-6">Select a Game</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    {games.map((game) => (
                        <div
                            key={game.id}
                            className="group relative h-64 bg-[#2A1B12]/90 rounded-3xl p-8 border border-white/5 cursor-pointer hover:border-[#EBC98F]/50 transition-all overflow-hidden"
                        >
                            <div className="flex flex-col h-full justify-between relative z-10">
                                <div className="w-16 h-16 bg-[#5A3822] rounded-2xl flex items-center justify-center text-4xl font-bold text-[#EBC98F]">
                                    {game.title[0]}
                                </div>
                                <div>
                                    <h3 className="text-2xl font-bold text-[#EBC98F] mb-2">{game.title}</h3>
                                    <p className="text-white/60">{game.description}</p>
                                </div>
                            </div>
                            {/* Decorative background element */}
                            <div className="absolute top-0 right-0 w-32 h-32 bg-white/5 rounded-bl-full -mr-8 -mt-8 group-hover:scale-110 transition-transform" />
                        </div>
                    ))}
                </div>
            </main>

            {/* Hub Chat Area */}
            <footer className="mt-8 h-48 bg-[#2A1B12] rounded-3xl p-6 border border-white/5">
                <div className="flex justify-between items-center mb-4">
                    <h4 className="text-xs font-bold text-[#EBC98F] tracking-widest uppercase">Global Chat</h4>
                    <MoreHorizontal className="w-4 h-4 text-[#EBC98F]" />
                </div>
                <div className="text-sm text-white/40 flex items-center justify-center h-24 italic">
                    Select a game or chat room to start messaging
                </div>
            </footer>
        </div>
    );
}
