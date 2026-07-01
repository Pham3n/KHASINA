"use client";

import { Card, Suit } from "@/lib/game/GameEngine";
import { cn } from "@/lib/utils";

interface PlayingCardProps {
    card: Card;
    isSelected?: boolean;
    onClick?: () => void;
}

export function PlayingCard({ card, isSelected, onClick }: PlayingCardProps) {
    const isRed = card.suit === Suit.HEARTS || card.suit === Suit.DIAMONDS;

    const suitSymbol = {
        [Suit.SPADES]: "♠",
        [Suit.DIAMONDS]: "♦",
        [Suit.HEARTS]: "♥",
        [Suit.CLUBS]: "♣"
    }[card.suit];

    const rankText = card.rank === 1 ? "A" : card.rank.toString();

    return (
        <div
            onClick={onClick}
            className={cn(
                "relative w-[100px] h-[140px] bg-[#F3E3C3] rounded-xl cursor-pointer border-2 transition-all",
                isSelected ? "border-[#EBC98F] scale-105 shadow-xl shadow-yellow-500/20" : "border-white/10",
                !onClick && "cursor-default"
            )}
        >
            {/* Top Left */}
            <div className={cn("absolute top-2 left-2 text-sm font-bold", isRed ? "text-red-600" : "text-black")}>
                {rankText}
            </div>

            {/* Center */}
            <div className={cn("absolute inset-0 flex flex-col items-center justify-center", isRed ? "text-red-600" : "text-black")}>
                <span className="text-4xl leading-none">{suitSymbol}</span>
                <span className="text-xl font-bold">{rankText}</span>
            </div>

            {/* Bottom Right */}
            <div className={cn("absolute bottom-2 right-2 text-sm font-bold rotate-180", isRed ? "text-red-600" : "text-black")}>
                {rankText}
            </div>
        </div>
    );
}
