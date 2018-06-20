/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.me.unows.UnoWS;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

/**
 *
 * @author Rodrigo
 */
@WebService(serviceName = "UnoWS")
public class UnoWS {

    private static final int MATCH_CAPACITY = 500;

    enum MatchState {
        WaitStart,
        InProgress,
        Finished
    }

    class Match {

        int userId1 = 0, userId2 = 0;
        MatchState state;
        Uno game;
        Date lastTime;

        Match() {
            state = MatchState.WaitStart;
        }

        int getUserId1() {
            return userId1;
        }

        void setUserId1(int userId1) {
            this.userId1 = userId1;
        }

        int getUserId2() {
            return userId2;
        }

        void setUserId2(int userId2) {

            this.userId2 = userId2;

            // Start the match
            game = new Uno(userId1, userId2);
            state = MatchState.InProgress;

        }

    }
    
    

    private final HashMap<Integer, String> dictUserIdNick;
    private HashMap<Integer, Integer> dictUserIdMatche;
    private final Match[] matches;

    public UnoWS() {

        matches = new Match[MATCH_CAPACITY];
        dictUserIdNick = new HashMap<>(MATCH_CAPACITY * 2);
        dictUserIdMatche = new HashMap<>(MATCH_CAPACITY * 2);

    }

    @WebMethod(operationName = "registraJogador")
    public int registraJogador(@WebParam(name = "Nickname") String nick) {
        // TODO: Remover partidas encerradas ha 120 segundas (erro: if da data errado)
        // remover partidas encerradas ha mais de 120 segundas
        /*long now = new Date().getTime() - 120 * 1000;
        for (int i = 0; i < MATCH_CAPACITY; i++) {
            Match m = matches[i];
            if (m != null && m.state == MatchState.Finished && m.lastTime.getTime() < now) {
                matches[i] = null;
                dictUserIdMatche.remove(m.userId1);
                dictUserIdMatche.remove(m.userId2);
                dictUserIdNick.remove(m.userId1);
                dictUserIdNick.remove(m.userId2);
            }
        }*/

        if (dictUserIdNick.containsValue(nick)) {
            return -1; // Usuario ja cadastrado
        }

        for (int i = 0; i < MATCH_CAPACITY; i++) {
            Match match = matches[i];
            if (match == null || match.state == MatchState.WaitStart) {

                Random r = new Random();
                int id = r.nextInt();
                while (id <= 0 || dictUserIdNick.containsKey(id)) { // Id must be great than zero
                    id = r.nextInt();
                }

                dictUserIdNick.put(id, nick);
                dictUserIdMatche.put(id, i);

                if (match == null) {
                    match = new Match();
                    matches[i] = match;
                    match.setUserId1(id);
                    matches[i] = match;
                } else {
                    match.setUserId2(id);
                }

                System.out.println("Id " + id + " registrado para jogador " + nick);

                // Success
                return id;

            }
        }

        return -2; // Server is full
    }

    @WebMethod(operationName = "encerraPartida")
    public int encerraPartida(@WebParam(name = "IdUsuario") int idUsuario) {
        if (dictUserIdMatche.containsKey(idUsuario)) {
            Match match = matches[dictUserIdMatche.get(idUsuario)];

            if (match.state == MatchState.InProgress) {
                match.state = MatchState.Finished;
                match.lastTime = new Date();
                return match.game.encerraPartida(idUsuario);
            }
        }

        return -1; // erro
    }

    @WebMethod(operationName = "temPartida")
    public int temPartida(@WebParam(name = "IdUsuario") int idUsuario) {
        //TODO: temPartida return ­2 (tempo de espera esgotado)

        if (dictUserIdMatche.containsKey(idUsuario)) { // verifica se ha partida para usuario
            Match match = matches[dictUserIdMatche.get(idUsuario)];

            if (match.state == MatchState.WaitStart) {

                return 0; // ainda nao ha partida disponivel

            } else if (match.state == MatchState.InProgress) {
                if (idUsuario == match.getUserId1()) {
                    return 1; // ha partida e jogador 1
                } else {
                    return 2; // ha partida e jogador 2
                }
            }

        }

        return -1;
    }

    @WebMethod(operationName = "obtemOponente")
    public String obtemOponente(@WebParam(name = "IdIsuario") int idIsuario) {
        int i = temPartida(idIsuario);
        if (i > 0) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            if (i == 1) { // sou usuario 1 entao retorna usuario 2
                if (match.getUserId2() != 0) {
                    return dictUserIdNick.get(match.getUserId2());
                }
            } else { // sou usuario e entao retorna usuario 1
                if (match.getUserId1() != 0) {
                    return dictUserIdNick.get(match.getUserId1());
                }
            }
        }

        return ""; // Erro

    }

    @WebMethod(operationName = "ehMinhaVez")
    public int ehMinhaVez(@WebParam(name = "IdIsuario") int idIsuario) {
        if (dictUserIdMatche.containsKey(idIsuario)) { // verifica se ha partida para usuario

            Match match = matches[dictUserIdMatche.get(idIsuario)];
            if (match.state == MatchState.WaitStart) {

                return -2; // ainda nao ha partida disponivel

            } else if (match.state == MatchState.InProgress) {

                if (match.game.getNextPlayer() == idIsuario) {
                    return 1;
                } else {
                    return 0;
                }

            } else if (match.state == MatchState.Finished) {

                if (match.game.campeao == 0) {
                    return 4; // houve empate
                }
                if (match.game.campeao == idIsuario) {
                    if (!match.game.isWO) {
                        return 2; // é o vencedor 
                    } else {
                        return 5; // vencedor por WO 
                    }
                } else {
                    if (!match.game.isWO) {
                        return 3; // é o perdedor
                    } else {
                        return 6; // perdedor por WO
                    }
                }
            }

        }

        return -1; // error
    }

    @WebMethod(operationName = "obtemNumCartasBaralho")
    public int obtemNumCartasBaralho(@WebParam(name = "IdIsuario") int idIsuario) {
        int r = temPartida(idIsuario);
        if (r == 0) {
            return -2; // nao ha dois jogadores
        } else if (r > 0) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            return match.game.getNumCartasBaralho();
        } else {
            return -1;
        }
    }

    @WebMethod(operationName = "obtemNumCartas")
    public int obtemNumCartas(@WebParam(name = "IdIsuario") int idIsuario) {
        int r = temPartida(idIsuario);
        if (r == 0) {
            return -2; // nao ha dois jogadores
        } else if (r > 0) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            return match.game.getNumCartas(idIsuario, false);
        } else {
            return -1;
        }

    }

    @WebMethod(operationName = "obtemNumCartarOponente")
    public int obtemNumCartarOponente(@WebParam(name = "IdIsuario") int idIsuario) {
        int r = temPartida(idIsuario);
        if (r == 0) {
            return -2; // nao ha dois jogadores
        } else if (r > 0) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            return match.game.getNumCartas(idIsuario, true);
        } else {
            return -1;
        }
    }

    @WebMethod(operationName = "mostraMao")
    public String mostraMao(@WebParam(name = "IdIsuario") int idIsuario) {
        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            return match.game.getCartas(idIsuario);
        }
        return null;
    }

    @WebMethod(operationName = "obtemCartaMesa")
    public String obtemCartaMesa(@WebParam(name = "IdIsuario") int idIsuario) {
        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            return match.game.getCartaMesa();
        }
        return null;
    }

    @WebMethod(operationName = "obtemCorAtiva")
    public int obtemCorAtiva(@WebParam(name = "IdIsuario") int idIsuario) {
        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            return match.game.getCor();
        }
        return -1;
    }

    @WebMethod(operationName = "compraCarta")
    public int compraCarta(@WebParam(name = "IdIsuario") int idIsuario) {
        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            return match.game.compraCarta(idIsuario);
        }
        return -1;
    }

    @WebMethod(operationName = "jogaCarta")
    public int jogaCarta(@WebParam(name = "IdIsuario") int idIsuario, @WebParam(name = "IdxCarta") int idxCarta, @WebParam(name = "Cor") int cor) {
        //TODO: int jogaCarta(int idIsuario, int idxCarta, int cor)

        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            if (match.state == MatchState.WaitStart) {
                return -2; // ­2 (partida não iniciada: ainda não há dois jogadores registrados na partida)
            } else {

                //  1 (tudo certo), 
                //  0 (jogada inválida: por exemplo, a carta não corresponde à cor que está na mesa)
                // ­3 (parâmetros inválidos)
                // ­4 (não é a vez do jogador).
                return match.game.jogaCarta(idIsuario, idxCarta, cor);

            }
        }

        return -1; // ­1 (jogador não encontrado)
    }

    @WebMethod(operationName = "obtemPontos")
    public int obtemPontos(@WebParam(name = "IdIsuario") int idIsuario) {
        return obtemPontosAux(idIsuario, false);
    }

    @WebMethod(operationName = "obtemPontosOponente")
    public int obtemPontosOponente(@WebParam(name = "IdIsuario") int idIsuario) {
        return obtemPontosAux(idIsuario, true);
    }

    @WebMethod(operationName = "passarVez")
    public int passarVez(@WebParam(name = "IdUsuario") int idUsuario) {
        if (dictUserIdMatche.containsKey(idUsuario)) {
            Match match = matches[dictUserIdMatche.get(idUsuario)];
            return match.game.passaVez(idUsuario);
        }
        return -1;
    }

    private int obtemPontosAux(int idIsuario, boolean oponente) {
        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            if (match.state == MatchState.Finished) {
                return match.game.getPontosJogador(idIsuario, oponente);
            } else if (match.state == MatchState.WaitStart) {
                return -2; // partida ainda não iniciada
            } else {
                return -3; // partida não finalizada
            }
        }

        return -1; // Jogador nao encontrado
    }

}
