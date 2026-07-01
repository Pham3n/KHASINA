export enum Suit {
    SPADES = "SPADES",
    DIAMONDS = "DIAMONDS",
    HEARTS = "HEARTS",
    CLUBS = "CLUBS"
}

export interface Card {
    rank: number;
    suit: Suit;
}

export function getCardValue(card: Card): number {
    return card.rank;
}

export function getCardPoints(card: Card): number {
    if (card.rank === 1) return 1; // Ace
    if (card.rank === 10 && card.suit === Suit.DIAMONDS) return 2; // Big Casino
    if (card.rank === 2 && card.suit === Suit.SPADES) return 1; // Little Casino
    return 0;
}

export interface Construction {
    ownerIndex: number;
    targetValue: number;
    cards: Card[];
}

export class GameEngine {
    deck: Card[] = [];
    hands: Card[][] = [];
    floor: Card[] = [];
    privateStacks: Card[][] = [];
    constructions: Construction[] = [];
    currentPlayerIndex: number = 0;
    gameOver: boolean = false;
    useAI: boolean = true;

    constructor(public playerCount: number = 2) {
        this.setupGame();
    }

    setupGame() {
        const allCards: Card[] = [];
        const suits = [Suit.SPADES, Suit.DIAMONDS, Suit.HEARTS, Suit.CLUBS];
        for (const suit of suits) {
            for (let rank = 1; rank <= 10; rank++) {
                allCards.push({ rank, suit });
            }
        }

        // Fisher-Yates shuffle
        for (let i = allCards.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [allCards[i], allCards[j]] = [allCards[j], allCards[i]];
        }

        this.deck = allCards;
        this.floor = [];
        this.constructions = [];
        this.hands = Array.from({ length: this.playerCount }, () => []);
        this.privateStacks = Array.from({ length: this.playerCount }, () => []);

        this.deal();
        this.currentPlayerIndex = 0;
        this.gameOver = false;
    }

    deal() {
        if (this.playerCount === 4) {
            for (let i = 0; i < 10; i++) {
                for (let p = 0; p < 4; p++) {
                    if (this.deck.length > 0) this.hands[p].push(this.deck.shift()!);
                }
            }
        } else if (this.playerCount === 3) {
            for (let i = 0; i < 3; i++) {
                for (let p = 0; p < 3; p++) {
                    if (this.deck.length > 0) this.hands[p].push(this.deck.shift()!);
                }
            }
            if (this.floor.length === 0 && this.deck.length > 0) {
                this.floor.push(this.deck.shift()!);
            }
        } else {
            for (let i = 0; i < 10; i++) {
                for (let p = 0; p < 2; p++) {
                    if (this.deck.length > 0) this.hands[p].push(this.deck.shift()!);
                }
            }
        }

        if (this.playerCount !== 3 && this.floor.length === 0 && this.deck.length > 0) {
            for (let i = 0; i < 4; i++) {
                if (this.deck.length > 0) this.floor.push(this.deck.shift()!);
            }
        }
    }

    drawCard(playerIndex: number) {
        if (this.deck.length > 0 && this.hands[playerIndex].length < 3) {
            this.hands[playerIndex].push(this.deck.shift()!);
        }
    }

    executePlay(playedCard: Card, playerIndex: number) {
        this.removeCardFromHand(playedCard, playerIndex);
        if (this.playerCount === 3) this.drawCard(playerIndex);
        this.checkDisassemble(playerIndex, playedCard);
        this.floor.push(playedCard);
        this.nextTurn();
    }

    private removeCardFromHand(card: Card, playerIndex: number) {
        const hand = this.hands[playerIndex];
        const idx = hand.findIndex(c => c.rank === card.rank && c.suit === card.suit);
        if (idx !== -1) hand.splice(idx, 1);
    }

    private checkDisassemble(playerIndex: number, playedCard: Card) {
        const myConstruction = this.constructions.find(c => c.ownerIndex === playerIndex);
        if (!myConstruction) return;

        if (playedCard.rank === myConstruction.targetValue) {
            const remainingInHand = this.hands[playerIndex].filter(c => c.rank === myConstruction.targetValue).length;
            if (remainingInHand === 0) {
                this.floor.push(...myConstruction.cards);
                this.constructions = this.constructions.filter(c => c !== myConstruction);
            }
        }
    }

    executeBuild(
        playedCard: Card,
        selectedFloorCards: Card[],
        selectedConstructions: Construction[],
        recoveredOpponentCard: Card | null,
        playerIndex: number
    ): boolean {
        const hand = this.hands[playerIndex];
        const partnerIndex = this.playerCount === 4 ? (playerIndex + 2) % 4 : -1;

        const floorVal = selectedFloorCards.reduce((sum, c) => sum + c.rank, 0);
        const constructVal = selectedConstructions.reduce((sum, c) => sum + c.targetValue, 0);
        const oppVal = recoveredOpponentCard?.rank || 0;
        const sum = floorVal + constructVal + oppVal + playedCard.rank;

        let targetConstruction = this.constructions.find(c => c.ownerIndex === playerIndex);
        if (!targetConstruction && partnerIndex !== -1) {
            targetConstruction = this.constructions.find(c => c.ownerIndex === partnerIndex);
        }

        const targetValue = targetConstruction ? targetConstruction.targetValue : (() => {
            const remainingHand = [...hand].filter(c => !(c.rank === playedCard.rank && c.suit === playedCard.suit));
            const possibleValues = [...new Set(remainingHand.map(c => c.rank))];
            const filtered = possibleValues.filter(v => sum % v === 0);
            return filtered.length > 0 ? Math.max(...filtered) : -1;
        })();

        const hasTargetInHand = hand.some(c => c.rank === targetValue && !(c.rank === playedCard.rank && c.suit === playedCard.suit));

        if (targetValue > 0 && hasTargetInHand && sum % targetValue === 0) {
            if (!targetConstruction) {
                targetConstruction = { ownerIndex: playerIndex, targetValue, cards: [] };
                this.constructions.push(targetConstruction);
            }

            targetConstruction.cards.push(...selectedFloorCards);
            for (const c of selectedConstructions) {
                if (c !== targetConstruction) {
                    targetConstruction.cards.push(...c.cards);
                    this.constructions = this.constructions.filter(item => item !== c);
                }
            }

            if (recoveredOpponentCard) {
                targetConstruction.cards.push(recoveredOpponentCard);
                for (const stack of this.privateStacks) {
                    const idx = stack.findIndex(c => c.rank === recoveredOpponentCard.rank && c.suit === recoveredOpponentCard.suit);
                    if (idx !== -1) stack.splice(idx, 1);
                }
            }

            targetConstruction.cards.push(playedCard);
            this.floor = this.floor.filter(c => !selectedFloorCards.includes(c));
            this.removeCardFromHand(playedCard, playerIndex);
            if (this.playerCount === 3) this.drawCard(playerIndex);
            return true;
        }

        this.executePlay(playedCard, playerIndex);
        return false;
    }

    executeCapture(
        playedCard: Card,
        selectedFloorCards: Card[],
        selectedConstructions: Construction[],
        recoveredOpponentCard: Card | null,
        playerIndex: number
    ): boolean {
        const floorVal = selectedFloorCards.reduce((sum, c) => sum + c.rank, 0);
        const constructVal = selectedConstructions.reduce((sum, c) => sum + c.targetValue, 0);
        const oppVal = recoveredOpponentCard?.rank || 0;
        const totalSelectedValue = floorVal + constructVal + oppVal;

        if (totalSelectedValue > 0 && totalSelectedValue % playedCard.rank === 0) {
            if (selectedConstructions.some(c => c.targetValue !== playedCard.rank)) return false;

            const stack = this.privateStacks[playerIndex];
            const allCaptured: Card[] = [...selectedFloorCards];
            selectedConstructions.forEach(c => allCaptured.push(...c.cards));

            if (recoveredOpponentCard) {
                allCaptured.push(recoveredOpponentCard);
                for (const s of this.privateStacks) {
                    const idx = s.findIndex(c => c.rank === recoveredOpponentCard.rank && c.suit === recoveredOpponentCard.suit);
                    if (idx !== -1) s.splice(idx, 1);
                }
            }

            stack.push(...allCaptured, playedCard);
            this.floor = this.floor.filter(c => !selectedFloorCards.includes(c));
            this.constructions = this.constructions.filter(c => !selectedConstructions.includes(c));
            this.removeCardFromHand(playedCard, playerIndex);
            if (this.playerCount === 3) this.drawCard(playerIndex);
            return true;
        }
        return false;
    }

    nextTurn() {
        if (!this.gameOver) {
            this.currentPlayerIndex = (this.currentPlayerIndex + 1) % this.playerCount;
            if (this.hands.every(h => h.length === 0) && this.deck.length === 0) {
                this.gameOver = true;
            }
        }
    }

    calculateScores(): Record<string, number> {
        const scores: Record<string, number> = {};

        if (this.playerCount === 3) {
            for (let i = 0; i < 3; i++) {
                scores[`Team${i}`] = this.privateStacks[i].reduce((sum, c) => sum + getCardPoints(c), 0);
            }
            return scores;
        }

        const team0Stack = this.playerCount === 4 ? [...this.privateStacks[0], ...this.privateStacks[2]] : this.privateStacks[0];
        const team1Stack = this.playerCount === 4 ? [...this.privateStacks[1], ...this.privateStacks[3]] : this.privateStacks[1];

        let s0 = team0Stack.reduce((sum, c) => sum + getCardPoints(c), 0);
        let s1 = team1Stack.reduce((sum, c) => sum + getCardPoints(c), 0);

        if (team0Stack.length > team1Stack.length) s0 += 1;
        else if (team1Stack.length > team0Stack.length) s1 += 1;

        if (team0Stack.filter(c => c.suit === Suit.SPADES).length >= 6) s0 += 1;
        if (team1Stack.filter(c => c.suit === Suit.SPADES).length >= 6) s1 += 1;

        scores["Team0"] = s0;
        scores["Team1"] = s1;
        return scores;
    }
}
