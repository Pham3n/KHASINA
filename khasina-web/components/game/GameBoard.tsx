"use client";

import { useGameStore } from "@/lib/game/useGameStore";
import { PlayingCard } from "./PlayingCard";
import { cn } from "@/lib/utils";

export default function GameBoard() {
    const {
        engine,
        selectedCardHand,
        selectedCardsFloor,
        selectCardHand,
        toggleCardFloor
    } = useGameStore();

    return (
        <div className="flex flex-col h-full w-full gap-8">
            {/* Top Info / Opponents */}
            <div className="flex justify-between items-start">
                <div className="flex gap-4">
                    <div className="bg-[#2A1B12] p-4 rounded-2xl border border-white/5">
                        <div className="text-[10px] font-bold text-[#EBC98F] uppercase tracking-wider mb-1">Round</div>
                        <div className="text-xl font-bold">1 / 2</div>
                    </div>
                    <div className="bg-[#2A1B12] p-4 rounded-2xl border border-white/5 min-w-[120px]">
                        <div className="text-[10px] font-bold text-[#EBC98F] uppercase tracking-wider mb-1">Turn</div>
                        <div className="text-xl font-bold uppercase tracking-tight">You</div>
                    </div>
                </div>

                {/* AI / Opponent Panel */}
                <div className="bg-[#2A1B12] p-4 rounded-2xl border border-white/5 flex gap-4 items-center">
                    <div className="w-12 h-12 bg-[#5A3822] rounded-full" />
                    <div>
                        <div className="text-lg font-bold text-[#EBC98F]">AI</div>
                        <div className="text-xs text-white/40">10 Cards • Score: 0</div>
                    </div>
                </div>
            </div>

            {/* Middle: The Floor */}
            <div className="flex-1 bg-[#6A4528]/40 rounded-[40px] p-8 border border-white/5 relative shadow-inner overflow-hidden">
                <div className="absolute top-6 left-8 text-xs font-bold text-[#EBC98F] uppercase tracking-[0.2em] opacity-50">Floor</div>

                <div className="flex flex-wrap gap-4 justify-center items-center h-full">
                    {engine.floor.map((card, idx) => (
                        <PlayingCard
                            key={idx}
                            card={card}
                            isSelected={selectedCardsFloor.includes(card)}
                            onClick={() => toggleCardFloor(card)}
                        />
                    ))}
                    {engine.floor.length === 0 && (
                        <div className="text-white/20 text-sm italic">Floor is empty</div>
                    )}
                </div>
            </div>

            {/* Bottom: Player Hand */}
            <div className="flex flex-col gap-4">
                <div className="flex justify-between items-center">
                    <h3 className="text-xl font-bold text-[#EBC98F]">Your Hand</h3>
                    <div className="flex gap-2">
                        <button className="px-6 py-3 bg-[#5A3822] rounded-xl font-bold text-[#EBC98F] hover:bg-[#6A4528] transition-colors disabled:opacity-50">CAPTURE</button>
                        <button className="px-6 py-3 bg-[#5A3822] rounded-xl font-bold text-[#EBC98F] hover:bg-[#6A4528] transition-colors disabled:opacity-50">BUILD</button>
                        <button className="px-6 py-3 bg-[#8B5E3C] rounded-xl font-bold text-white hover:bg-[#9B6E4C] transition-colors">PLAY</button>
                    </div>
                </div>

                <div className="flex gap-4 overflow-x-auto pb-4">
                    {engine.hands[0].map((card, idx) => (
                        <PlayingCard
                            key={idx}
                            card={card}
                            isSelected={selectedCardHand === card}
                            onClick={() => selectCardHand(card)}
                        />
                    ))}
                </div>
            </div>
        </div>
    );
}
