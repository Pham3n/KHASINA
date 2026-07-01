import { create } from "zustand";
import { Card, Construction, GameEngine } from "./GameEngine";

interface GameState {
    engine: GameEngine;
    selectedCardHand: Card | null;
    selectedCardsFloor: Card[];
    selectedConstructions: Construction[];
    selectedOpponentStackCard: Card | null;
    isMultiplayer: boolean;
    isLocalAiEnabled: boolean;
    currentRound: number;
    maxRounds: number;
    cumulativeScores: number[];
    isHandRevealed: boolean;

    // Actions
    resetLocalGame: () => void;
    selectCardHand: (card: Card | null) => void;
    toggleCardFloor: (card: Card) => void;
    toggleConstruction: (c: Construction) => void;
    setHandRevealed: (revealed: boolean) => void;
}

export const useGameStore = create<GameState>((set) => ({
    engine: new GameEngine(2),
    selectedCardHand: null,
    selectedCardsFloor: [],
    selectedConstructions: [],
    selectedOpponentStackCard: null,
    isMultiplayer: false,
    isLocalAiEnabled: true,
    currentRound: 1,
    maxRounds: 2,
    cumulativeScores: [0, 0, 0, 0],
    isHandRevealed: true,

    resetLocalGame: () => set((state) => ({
        engine: new GameEngine(state.engine.playerCount),
        selectedCardHand: null,
        selectedCardsFloor: [],
        selectedConstructions: [],
        selectedOpponentStackCard: null,
        currentRound: 1,
        cumulativeScores: [0, 0, 0, 0],
        isHandRevealed: true
    })),

    selectCardHand: (card) => set({ selectedCardHand: card }),

    toggleCardFloor: (card) => set((state) => ({
        selectedCardsFloor: state.selectedCardsFloor.includes(card)
            ? state.selectedCardsFloor.filter(c => c !== card)
            : [...state.selectedCardsFloor, card]
    })),

    toggleConstruction: (c) => set((state) => ({
        selectedConstructions: state.selectedConstructions.includes(c)
            ? state.selectedConstructions.filter(item => item !== c)
            : [...state.selectedConstructions, c]
    })),

    setHandRevealed: (revealed) => set({ isHandRevealed: revealed })
}));
