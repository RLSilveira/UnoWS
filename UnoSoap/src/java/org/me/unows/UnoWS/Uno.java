/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.me.unows.UnoWS;

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
    int campeao = 0;

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

        //TODO: verificar cartas de acao como primeira carta
    }

    int getNextPlayer() {

        return next ? userId1 : userId2;

    }

    String getCartaMesa() {
        return cartaToString(cartaNaMesa);
    }

    int getNumCartasBaralho() {
        return numCartas;
    }

    int getNumCartas(int idUsuario, boolean oponente) {

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

    int getCorCarta(int carta) {
        int cor = carta / 25;
        return cor;
    }

    int getNumCarta(int carta) {
        int cor = carta / 25;
        int aux = carta - (cor * 25);
        int valor = (aux + 1) / 2;
        return valor;
    }

    String cartaToString(int carta) {

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

    public String getCartas(int idIsuario) {
        LinkedList<Integer> cartas = null;
        if (idIsuario == userId1) {
            cartas = cartasP1;
        } else if (idIsuario == userId2) {
            cartas = cartasP2;
        }

        if (cartas == null || cartas.isEmpty()) {
            return null;
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

    public int getCor() {
        int cor = cartaNaMesa / 25;
        return cor;
    }

    public int compraCarta(int idIsuario) {

        if ((next && idIsuario != userId1)
                || (!next && idIsuario != userId2)) {
            return -3; // não é a vez do jogador
        }

        if (numCartas < 0) {
            encerraPartida(0);
        } else {
            next = !next; // passar a vez
            if (idIsuario == userId1) {
                cartasP1.add(baralho[--numCartas]);
                return 0;
            } else if (idIsuario == userId2) {
                cartasP2.add(baralho[--numCartas]);
                return 0;
            }
        }

        return -1;

    }

    public int getPontosJogador(int idIsuario, boolean oponente) {

        LinkedList<Integer> cartas = null;
        if (idIsuario == userId1) {
            if (oponente) {
                cartas = cartasP2;
            } else {
                cartas = cartasP1;
            }
        } else if (idIsuario == userId2) {
            if (oponente) {
                cartas = cartasP1;
            } else {
                cartas = cartasP2;
            }
        }

        if (cartas == null) {
            return Integer.MIN_VALUE;
        }

        int p = 0;
        for (int i : cartas) {
            p += getCartaValue(i);
        }

        return p;
    }

    int getCartaValue(int carta) {

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

    public int encerraPartida(int idUsuario) {

        if (idUsuario > 0) {

            // *** usuario desistiu
            isWO = true;
            campeao = idUsuario == userId1 ? userId2 : userId1;

        } else {

            int pontos1 = getPontosJogador(userId1, false);
            int pontos2 = getPontosJogador(userId2, false);

            if (pontos1 == pontos2){
                //empate
            }
            else{
                campeao = (pontos1 > pontos2 ? userId1 : userId2);
            }
        }

        match.state = UnoWS.MatchState.Finished;
        return 0;

    }

    public int jogaCarta(int idIsuario, int idxCarta, int cor) {

        if (!(next && idIsuario == userId1 || !next && idIsuario == userId2)) {
            return -3; // não é a vez do jogador.
        }

        LinkedList<Integer> cartasMao = next ? cartasP1 : cartasP2; // cartas do jogador

        if (idxCarta < 0 || idxCarta >= cartasMao.size()) {
            return -4; // parâmetros inválidos
        }

        int cartaSerJogada = cartasMao.get(idxCarta);

        //TODO: Verificar se pode jogar coringa em cima de coringa
        if (cartaSerJogada >= 100) { // coringa

            // *** carta na mesa é coringa 
            if (cor < 0 || cor > 3) {
                return -4; // parâmetros inválidos
            }

            corAtiva = cor; // cor escolhida pelo usuario

            if (cartaSerJogada >= 104) { // coringa +4 - comprar para oponente

                LinkedList<Integer> cartasOponente = !next ? cartasP1 : cartasP2;
                for (int i = 0; i < 4; i++) {
                    if (numCartas < 0) {
                        encerraPartida(0);
                    }
                    cartasOponente.add(baralho[--numCartas]);
                }
            }

        } else {

            // *** não é coringa, checar cor/numero
            int corCartaMesa = getCorCarta(cartaNaMesa);
            int numCartaMesa = getNumCarta(cartaNaMesa);

            int corCartaJogada = getCorCarta(cartaSerJogada);
            int numCartaJogada = getNumCarta(cartaSerJogada);

            if (corCartaMesa == corCartaJogada || numCartaMesa == numCartaJogada) {

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
                            if (numCartas < 0) {
                                encerraPartida(0);
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

        }

        // ***  Jogada valida
        cartaNaMesa = cartasMao.remove(idxCarta);
        next = !next;

        return 1; //  1 (tudo certo)

    }
}
