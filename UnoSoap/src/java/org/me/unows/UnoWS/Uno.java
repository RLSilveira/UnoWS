/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.me.unows.UnoWS;

import com.sun.tools.xjc.api.S2JJAXBModel;
import java.util.LinkedList;
import java.util.Random;

/**
 *
 * @author Rodrigo
 */
class Uno {

    Random gerador;

    final int totalCartas = 108;
    private int userId1 = 0, userId2 = 0;
    LinkedList<Integer> cartasP1, cartasP2;
    boolean next = true; // true -> p1
    int[] baralho;
    int numCartas;
    int cartaNaMesa;
    int corAtiva = -1;

    boolean isWO = false;
    int campeao = -1;

    UnoWS.Match match;

    Uno(int userId1, int userId2, UnoWS.Match match) {
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.match = match;

        gerador = new Random(userId1 + userId2);

        // cria baralho
        baralho = new int[totalCartas];
        for (int i = 0; i < totalCartas; ++i) {
            baralho[i] = i;
        }

        // embaralha cartas
        for (int c = 0; c < totalCartas; ++c) {
            int outra = gerador.nextInt(totalCartas);
            int aux = baralho[c];
            baralho[c] = baralho[outra];
            baralho[outra] = aux;
        }
        for (int c = 0; c < totalCartas * totalCartas; c++) {
            int c1 = gerador.nextInt(totalCartas);
            int c2 = gerador.nextInt(totalCartas);
            int aux = baralho[c1];
            baralho[c1] = baralho[c2];
            baralho[c2] = aux;
        }
        numCartas = totalCartas;

        // dar as cartas
        cartasP1 = new LinkedList<>();
        cartasP2 = new LinkedList<>();
        for (int i = 0; i < 7; i++) {
            cartasP1.add(baralho[--numCartas]);
            cartasP2.add(baralho[--numCartas]);
        }

        // carta da mesa
        cartaNaMesa = baralho[--numCartas];

        // cartas de acao como primeira carta
        while (cartaNaMesa >= 100 || getNumCarta(cartaNaMesa) >= 10) {

            if (cartaNaMesa >= 100) {
                cartaNaMesa = baralho[--numCartas];
            } else {
                switch (getNumCarta(cartaNaMesa)) {
                    case 10: // Pu
                    case 11: // In
                        // passar a vez
                        next = !next;
                        break;

                    case 12: // +2
                        // comprar 2 cartas para mim
                        LinkedList<Integer> cartasOponente = next ? cartasP1 : cartasP2;
                        for (int i = 0; i < 2; i++) {
                            cartasOponente.add(baralho[--numCartas]);
                        }
                        // passar a vez
                        next = !next;
                        break;

                    default:
                        break;
                }

                break;

            }

        }

        corAtiva = getCorCarta(cartaNaMesa);

    }

    synchronized int getNextPlayer() {

        return next ? userId1 : userId2;

    }

    synchronized String getCartaMesa() {
        return cartaToString(cartaNaMesa);
    }

    synchronized int getNumCartasBaralho() {
        return numCartas;
    }

    synchronized int getNumCartas(int idUsuario, boolean oponente) {

        if (idUsuario == userId1) {
            if (oponente) {
                return cartasP2.size();
            } else {
                return cartasP1.size();
            }
        } else {
            if (oponente) {
                return cartasP1.size();
            } else {
                return cartasP2.size();
            }
        }
    }

    synchronized int getCorCarta(int carta) {
        int cor = carta / 25;
        return cor;
    }

    private synchronized int getNumCarta(int carta) {

        if (carta >= 100) {
            return carta; // coringa
        }
        int cor = carta / 25;
        int aux = carta - (cor * 25);
        int valor = (aux + 1) / 2;
        return valor;
    }

    synchronized String cartaToString(int carta) {

        if (carta < 0 || carta >= 108) {
            return "ERROR";
        }

        int cor = getCorCarta(carta);
        int valor = getNumCarta(carta);

        String s1 = null, s2 = null;

        if (valor <= 9) {
            // numero
            s1 = "" + valor;
        } else {
            // especial
            switch (valor) {
                case 10:
                    s1 = "Pu";
                    break;
                case 11:
                    s1 = "In";
                    break;
                case 12:
                    s1 = "+2";
                    break;
                default:
                    break;
            }
        }

        switch (cor) {
            case 0: // azul
                s2 = "Az";
                break;
            case 1: // amarelo
                s2 = "Am";
                break;
            case 2: // verde
                s2 = "Vd";
                break;
            case 3: // vermelho
                s2 = "Vm";
                break;
            case 4: //coringa
                if (carta < 104) {
                    s1 = "Cg";
                } else {
                    s1 = "C4";
                }
                s2 = "*";
                break;
        }

        return s1 + "/" + s2;
    }

    public synchronized String getCartas(int idIsuario) {
        LinkedList<Integer> cartas = null;
        if (idIsuario == userId1) {
            cartas = cartasP1;
        } else if (idIsuario == userId2) {
            cartas = cartasP2;
        }

        if (cartas == null || cartas.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean b = false;
        for (int i : cartas) {
            if (b) {
                sb.append("|");
            } else {
                b = true;
            }
            sb.append(cartaToString(i));
        }

        return sb.toString();

    }

    public synchronized int getCorAtiva() {
        return corAtiva;
    }

    public synchronized int compraCarta(int idIsuario) {

        if ((next && idIsuario != userId1)
                || (!next && idIsuario != userId2)) {
            return -3; // não é a vez do jogador
        }

        if (numCartas <= 0) {
            return -1; // não é possivel comprar, acabou baralho
        }

        next = !next; // passar a vez

        if (idIsuario == userId1) {
            cartasP1.add(baralho[--numCartas]);
        } else if (idIsuario == userId2) {
            cartasP2.add(baralho[--numCartas]);
        }

        return 1; // suesso

    }

    public synchronized int getPontosJogador(int idUsuario, boolean isOponente) {

        LinkedList<Integer> cartas = null;
        boolean isJogador1 = idUsuario == userId1;
        boolean isJogador2 = idUsuario == userId2;

        if (isJogador1) {
            if (isOponente) {
                cartas = cartasP2;
            } else {
                cartas = cartasP1;
            }
        } else if (isJogador2) {
            if (isOponente) {
                cartas = cartasP1;
            } else {
                cartas = cartasP2;
            }
        }

        if (cartas == null) {
            
            System.out.println("getPontos: cartas isn null");
            System.out.println(String.format("solicitado por usuario: %d", idUsuario));
            System.out.println(String.format("partida entre {%d %d}", userId1, userId2));
            System.out.println(String.format("baralho: %d, %s", numCartas, cartaToString(cartaNaMesa)));
            System.out.println(String.format("mao P1: %s", getCartas(userId1)));
            System.out.println(String.format("mao P2: %s", getCartas(userId2)));
            System.out.println(String.format("campeao: %d W.O.:%d", campeao, isWO ? 1 : 0));
            System.out.println(String.format("match state: %s", match.state));

            return Integer.MIN_VALUE - 1;
        }

        int p = 0;
        for (int i : cartas) {
            p += getCartaValue(i);
        }

        return p;
    }

    synchronized int getCartaValue(int carta) {

        if (carta < 0 || carta >= 108) {
            return Integer.MAX_VALUE;
        }

        int cor = carta / 25;
        int aux = carta - (cor * 25);
        int valor = (aux + 1) / 2;

        if (cor > 3) {
            return 50; // coringa
        }
        if (valor >= 10) {
            return 20; // cartas de ação
        }
        return valor;

    }

    public synchronized int encerraPartida(int idUsuario) {

        if (idUsuario > 0) {

            // *** usuario desistiu
            isWO = true;
            campeao = idUsuario == userId1 ? userId2 : userId1;

        } else {

            // *** partida acabou
            if (cartasP1.isEmpty()) {
                campeao = userId1;
            } else if (cartasP2.isEmpty()) {
                campeao = userId2;
            } else {

                // *** calcular pontos
                int pontosP1 = getPontosJogador(userId1, false);
                int pontosP2 = getPontosJogador(userId2, false);

                if (pontosP1 == pontosP2) {
                    campeao = 0; //empate
                } else {
                    campeao = pontosP1 < pontosP2 ? userId1 : userId2;
                }
            }

        }

        match.state = UnoWS.MatchState.Finished;
        return 0;

    }

    public synchronized int jogaCarta(int idIsuario, int idxCarta, int cor) {

        if (!(next && idIsuario == userId1 || !next && idIsuario == userId2)) {
            return -3; // não é a vez do jogador.
        }

        LinkedList<Integer> cartasMao = next ? cartasP1 : cartasP2; // cartas do jogador

        if (idxCarta < 0 || idxCarta >= cartasMao.size()) {
            return -4; // parâmetros inválidos
        }

        int cartaSerJogada = cartasMao.get(idxCarta);

        if (cartaSerJogada >= 100) { // coringa

            // *** carta na mesa é coringa 
            if (cor < 0 || cor > 3) {
                return -4; // parâmetros inválidos
            }

            corAtiva = cor; // cor escolhida pelo usuario

            if (cartaSerJogada >= 104) { // coringa +4 - comprar para oponente

                LinkedList<Integer> cartasOponente = !next ? cartasP1 : cartasP2;
                for (int i = 0; i < 4; i++) {
                    if (numCartas <= 0) {
                        encerraPartida(0);
                        break;
                    }
                    cartasOponente.add(baralho[--numCartas]);
                }
            }

        } else {

            // *** não é coringa, checar cor/numero
            int numCartaMesa = getNumCarta(cartaNaMesa);

            int corCartaJogada = getCorCarta(cartaSerJogada);
            int numCartaJogada = getNumCarta(cartaSerJogada);

            if (corAtiva == corCartaJogada || numCartaMesa == numCartaJogada) {

                // *** jogada valida
                switch (numCartaJogada) {
                    case 10: // Pu
                    case 11: // In
                        // passar a vez
                        next = !next;
                        break;

                    case 12: // +2
                        // comprar 2 cartas para oponente
                        LinkedList<Integer> cartasOponente = !next ? cartasP1 : cartasP2;
                        for (int i = 0; i < 2; i++) {
                            if (numCartas <= 0) {
                                encerraPartida(0);
                                break;
                            }
                            cartasOponente.add(baralho[--numCartas]);
                        }
                        // passar a vez
                        next = !next;
                        break;

                    default:
                        break;
                }
            } else {

                // *** jogada invalida
                return 0; // 0 (jogada inválida: por exemplo, a carta não corresponde à cor que está na mesa)

            }

            corAtiva = corCartaJogada;

        }

        // ***  Jogada valida
        cartaNaMesa = cartasMao.remove(idxCarta);
        next = !next;

        if (cartasMao.isEmpty()) {
            // acabou as cartas, fim de jogo
            encerraPartida(0);
        }

        return 1; //  1 (tudo certo)

    }
}
