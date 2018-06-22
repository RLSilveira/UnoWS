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

    Uno(int userId1, int userId2) {
        this.userId1 = userId1;
        this.userId2 = userId2;

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
        if (numCartas > 0) {
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

    public int passaVez(int idIsuario) {
        next = !next;
        return 0;
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

    boolean isWO = false;
    int campeao = 0;

    public int encerraPartida(int idUsuario) {

        //isWO = true;
        campeao = idUsuario == userId1 ? userId2 : userId1;
        return 0;

    }

    public int jogaCarta(int idIsuario, int idxCarta, int cor) {

        //  0 (jogada inválida: por exemplo, a carta não corresponde à cor que está na mesa)
        if (!(next && idIsuario == userId1 || !next && idIsuario == userId2)) {
            return -4; // ­4 (não é a vez do jogador).
        }

        LinkedList<Integer> cartasMao = next ? cartasP1 : cartasP2;

        if (idxCarta < 0 || idxCarta >= cartasMao.size()) {
            return -3; // ­3 (parâmetros inválidos)
        }

        int cartaSerJogada = cartasMao.get(idxCarta);

        // carta na mesa é coringa
        int numCartaMesa = getNumCarta(cartaNaMesa);
        if (cartaSerJogada >= 100) { // coringa
            
            corAtiva = cor;

            if (cartaSerJogada >= 104) { // coringa +4
                LinkedList<Integer> cartasOponente = !next ? cartasP1 : cartasP2;
                if (numCartas < 0) {
                    encerraPartida(0);
                }
                cartasOponente.add(baralho[--numCartas]);
                if (numCartas < 0) {
                    encerraPartida(0);
                }
                cartasOponente.add(baralho[--numCartas]);
                if (numCartas < 0) {
                    encerraPartida(0);
                }
                cartasOponente.add(baralho[--numCartas]);
                if (numCartas < 0) {
                    encerraPartida(0);
                }
                cartasOponente.add(baralho[--numCartas]);
            }
        }
        else{
            // checar cor/numero
            
        }

        // ***  Jogada valida
        cartaNaMesa = cartasMao.remove(idxCarta);
        next = !next;

        return 1; //  1 (tudo certo)

    }
}
