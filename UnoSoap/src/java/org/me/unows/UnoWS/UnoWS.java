/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.me.unows.UnoWS;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
        Reserved,
        WaitStart,
        InProgress,
        Finished,
        Removed
    }

    class Match {

        int userId1 = 0, userId2 = 0;
        MatchState state;
        Uno game;

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
            game = new Uno(userId1, userId2, this);
            state = MatchState.InProgress;

        }

        boolean removeJogador(int userId) {
            if (userId == userId1) {
                userId1 = 0;
            } else if (userId == userId2) {
                userId2 = 0;
            }

            return userId1 + userId2 == 0; // true = 2 jogadores sairam, deve-se remover a partida

        }
    }

    private final HashMap<Integer, String> dictUserIdNick;
    private final HashMap<Integer, Integer> dictUserIdMatche;
    private final Match[] matches;

    //pre registro
    private final HashMap<String, Integer> dictPreRegUsernameUserId;
    private final HashMap<Integer, Integer> dictPreRegUserIdMatch;

    public UnoWS() {

        matches = new Match[MATCH_CAPACITY];
        dictUserIdNick = new HashMap<>(MATCH_CAPACITY * 2);
        dictUserIdMatche = new HashMap<>(MATCH_CAPACITY * 2);
        dictPreRegUserIdMatch = new HashMap<>(MATCH_CAPACITY * 2);
        dictPreRegUsernameUserId = new HashMap<>(MATCH_CAPACITY * 2);

    }

    /**
     * Operação de Web service
     */
    @WebMethod(operationName = "preRegistro")
    public synchronized Integer preRegistro(@WebParam(name = "username1") String username1, @WebParam(name = "userId1") int userId1, @WebParam(name = "username2") String username2, @WebParam(name = "userId2") int userId2) {

        dictPreRegUsernameUserId.put(username1, userId1);
        dictPreRegUsernameUserId.put(username2, userId2);

        // Find match slot free
        for (int idxMatch = 0; idxMatch < MATCH_CAPACITY; idxMatch++) {

            if (matches[idxMatch] == null || matches[idxMatch].state == MatchState.Removed) {
                // Found - reservar partida
                Match match = new Match();
                match.state = MatchState.Reserved;
                matches[idxMatch] = match;
                dictPreRegUserIdMatch.put(userId1, idxMatch);
                dictPreRegUserIdMatch.put(userId2, idxMatch);

                return 0;
            }
        }

        return 0;

    }

    @WebMethod(operationName = "registraJogador")
    public synchronized int registraJogador(@WebParam(name = "Nickname") String nick) {
        // TODO: Remover partidas encerradas ha 120 segundas (erro: if da data errado)

        if (dictUserIdNick.containsValue(nick)) {
            return -1; // Usuario ja cadastrado
        }

        int id;
        Match match = null;
        int idxMatch = -1;

        if (dictPreRegUsernameUserId.containsKey(nick)) {
            // *** nick em pre registro
            id = dictPreRegUsernameUserId.get(nick);

            if (!dictPreRegUserIdMatch.containsKey(id)) {

                return -2; // server is full

            } else {

                idxMatch = dictPreRegUserIdMatch.get(id);
                match = matches[idxMatch];

                dictPreRegUsernameUserId.remove(nick);
                dictPreRegUserIdMatch.remove(id);

            }

        } else {

            if (dictPreRegUsernameUserId.containsKey(nick)) {
                return -1; // Usuario ja cadastrado, no pre registro
            }

            Random r = new Random();
            id = r.nextInt();
            while (id <= 0 || dictUserIdNick.containsKey(id)) { // Id must be great than zero
                id = r.nextInt();
            }

        }

        if (match == null) {
            boolean bFound = false;
            for (idxMatch = 0; idxMatch < MATCH_CAPACITY; idxMatch++) {
                match = matches[idxMatch];
                if (match == null || match.state == MatchState.Removed || match.state == MatchState.WaitStart) {

                    if (match == null || match.state == MatchState.Removed) {
                        match = new Match();
                        matches[idxMatch] = match;
                    }
                    bFound = true;
                    break;
                }
            }

            if (!bFound) {
                return -2; // Server is full
            }

        }

        dictUserIdNick.put(id, nick);
        dictUserIdMatche.put(id, idxMatch);

        if (match.getUserId1() == 0) {
            match.setUserId1(id);
        } else {
            match.setUserId2(id);
        }

        // Success
        return id;
    }

    @WebMethod(operationName = "encerraPartida")
    public synchronized int encerraPartida(@WebParam(name = "IdUsuario") int idUsuario) {

        if (dictUserIdMatche.containsKey(idUsuario)) {
            int idxMatch = dictUserIdMatche.get(idUsuario);

            dictUserIdMatche.remove(idUsuario);
            dictUserIdNick.remove(idUsuario);

            if (null == matches[idxMatch].state) {

                // não deve cair aqui
                return -1;

            } else {

                if (matches[idxMatch].state == MatchState.InProgress) {

                    // *** encerrar partida por W.O.
                    matches[idxMatch].game.encerraPartida(idUsuario);

                }

                matches[idxMatch].state = MatchState.Finished;

                if (matches[idxMatch].removeJogador(idUsuario)) {

                    // remover partida do ultimo usuario a sair
                    // matches[idxMatch] = null;
                    matches[idxMatch].state = MatchState.Removed;

                }

                return 0;
            }
        }

        return -1;

    }

    @WebMethod(operationName = "temPartida")
    public synchronized int temPartida(@WebParam(name = "IdUsuario") int idUsuario) {
        //TODO: temPartida return ­2 (tempo de espera esgotado)

        if (dictUserIdMatche.containsKey(idUsuario)) { // verifica se ha partida para usuario
            Match match = matches[dictUserIdMatche.get(idUsuario)];

            if (match.state == MatchState.WaitStart || match.state == MatchState.Reserved) {

                return 0; // ainda nao ha partida disponivel

            } else {
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
    public synchronized String obtemOponente(@WebParam(name = "IdIsuario") int idIsuario) {
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
    public synchronized int ehMinhaVez(@WebParam(name = "IdIsuario") int idIsuario) {
        
        if (dictUserIdMatche.containsKey(idIsuario)) { // verifica se ha partida para usuario

            Match match = matches[dictUserIdMatche.get(idIsuario)];
            if (null != match.state) {
                switch (match.state) {
                    case WaitStart:
                    case Reserved:
                        return -2; // ainda nao ha partida disponivel
                    case InProgress:
                        if (match.game.getNextPlayer() == idIsuario) {
                            return 1;
                        } else {
                            return 0;
                        }
                    case Finished:
                        if (match.game.campeao == -1) {
                            return -1; // erro: campeao não definido
                        }
                        if (match.game.campeao == 0) {
                            return 4; // houve empate
                        }
                        if (match.game.campeao == idIsuario) {
                            if (match.game.isWO) {
                                return 5; // vencedor por WO 
                            } else {
                                return 2; // é o vencedor 
                            }
                        } else {
                            if (match.game.isWO) {
                                return 6; // perdedor por WO
                            } else {
                                return 3; // é o perdedor
                            }
                        }
                    default:
                        break;
                }
            }

        }

        return -1; // error
    }

    @WebMethod(operationName = "obtemNumCartasBaralho")
    public synchronized int obtemNumCartasBaralho(@WebParam(name = "IdIsuario") int idIsuario) {
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
    public synchronized int obtemNumCartas(@WebParam(name = "IdIsuario") int idIsuario) {
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

    @WebMethod(operationName = "obtemNumCartasOponente")
    public synchronized int obtemNumCartasOponente(@WebParam(name = "IdIsuario") int idIsuario) {
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
    public synchronized String mostraMao(@WebParam(name = "IdIsuario") int idIsuario) {
        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            if (match.state == MatchState.Finished || match.state == MatchState.InProgress) {
                return match.game.getCartas(idIsuario);
            }
        }
        return "";
    }

    @WebMethod(operationName = "obtemCartaMesa")
    public synchronized String obtemCartaMesa(@WebParam(name = "IdIsuario") int idIsuario) {
        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            if (match.state == MatchState.Finished || match.state == MatchState.InProgress) {
                return match.game.getCartaMesa();
            }
        }
        return null;
    }

    @WebMethod(operationName = "obtemCorAtiva")
    public synchronized int obtemCorAtiva(@WebParam(name = "IdIsuario") int idIsuario) {
        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            if (match.state == MatchState.Reserved || match.state == MatchState.WaitStart) {
                return -2; // erro: ainda não há 2 jogadores registrados na partida
            }
            return match.game.getCorAtiva();
        }
        return -1;
    }

    @WebMethod(operationName = "compraCarta")
    public synchronized int compraCarta(@WebParam(name = "IdIsuario") int idIsuario) {
        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            if (match.state == MatchState.Reserved || match.state == MatchState.WaitStart) {
                return -2; // partida não iniciada
            }
            //if (match.state == MatchState.InProgress) {
            return match.game.compraCarta(idIsuario);
            //}
        }
        return -1;
    }

    @WebMethod(operationName = "jogaCarta")
    public synchronized int jogaCarta(@WebParam(name = "IdIsuario") int idIsuario, @WebParam(name = "IdxCarta") int idxCarta, @WebParam(name = "Cor") int cor) {

        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];
            if (match.state == MatchState.WaitStart || match.state == MatchState.Reserved) {
                return -2; // ­2 (partida não iniciada: ainda não há dois jogadores registrados na partida)
            } else {
                return match.game.jogaCarta(idIsuario, idxCarta, cor);
            }
        }

        return -1; // ­1 (jogador não encontrado)
    }

    @WebMethod(operationName = "obtemPontos")
    public synchronized int obtemPontos(@WebParam(name = "IdIsuario") int idIsuario) {
        return obtemPontosAux(idIsuario, false);
    }

    @WebMethod(operationName = "obtemPontosOponente")
    public synchronized int obtemPontosOponente(@WebParam(name = "IdIsuario") int idIsuario) {
        return obtemPontosAux(idIsuario, true);
    }

    private synchronized int obtemPontosAux(int idIsuario, boolean oponente) {
        if (dictUserIdMatche.containsKey(idIsuario)) {
            Match match = matches[dictUserIdMatche.get(idIsuario)];

            if (match.state == MatchState.InProgress
                    || match.state == MatchState.Finished) {
                return match.game.getPontosJogador(idIsuario, oponente);
            } else {
                return -2; // partida ainda não iniciada
            }
        }

        return -1; // Jogador nao encontrado
    }

}
