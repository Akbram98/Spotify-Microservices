package com.eecs3311.profilemicroservice;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;
import java.util.*;
import java.util.Map.Entry;
import org.neo4j.driver.v1.Record;

import org.json.*;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;
	Map<String, List<String>> data;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			} catch (Exception e) {
				if (e.getMessage().contains("An equivalent constraint already exists")) {
					System.out.println("INFO: Playlist constraint already exist (DB likely already initialized), should be OK to continue");
				} else {
					// something else, yuck, bye
					throw e;
				}
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
		DbQueryStatus qStatus;
		
		String query  = "MATCH (nProfile: profile), (nSong: song) " +
		"WHERE nProfile.userName = '" + userName + "' AND nSong.songId = '" + songId + "' " +
		"CREATE (nProfile)-[:includes]->(nSong)";
		
		String query2 = "MATCH (nProfile1: profile {userName: '" + userName + "' })" +
				"-[r:includes]->(nSong: song {songId: '" + songId + "'})" +
				" RETURN nSong.songId";

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult rs = trans.run(query2);
				trans.success();
				if(!rs.hasNext()) {
					trans.run(query);
					trans.success();
					qStatus = new DbQueryStatus(userName + " liked song with songId: " + songId, DbQueryExecResult.QUERY_OK);
				}
				else
					qStatus = new DbQueryStatus(userName + " already liked song with songId: " + songId, DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

			} catch (Exception e) {
				session.close();
				qStatus = new DbQueryStatus("Query didn't execute", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return qStatus;
			}
			session.close();
		}

		return qStatus;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		DbQueryStatus qStatus;

		String query = "MATCH (nProfile1: profile {userName: '" + userName + "' })" +
				"-[r:includes]->(nSong: song {songId: '" + songId + "'})" +
				" DELETE r";
		
		String query2 = "MATCH (nProfile1: profile {userName: '" + userName + "' })" +
				"-[r:includes]->(nSong: song {songId: '" + songId + "'})" +
				" RETURN nSong.songId";

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult rs = trans.run(query2);
				trans.success();
				
				if(rs.hasNext()) {
					trans.run(query);
					trans.success();
					qStatus = new DbQueryStatus(userName + " unliked " + songId, DbQueryExecResult.QUERY_OK);
				}
				else
					qStatus = new DbQueryStatus(userName + " does not have " + songId + " as a liked song", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

			} catch (Exception e) {
				session.close();
				qStatus = new DbQueryStatus("Query didn't execute", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return qStatus;
			}
			session.close();
		}

		return qStatus;
	}

	public DbQueryStatus getPlaylists(String userName){
		DbQueryStatus qStatus;

		String query1 = "MATCH (nProfile: profile {userName: '" + userName + "'})-[:subscribes*1]->(fof)"
				+ " RETURN fof.plName";

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				data = new HashMap<String, List<String>>();

				StatementResult rs1 = trans.run(query1);
				trans.success();

				while(rs1.hasNext()) {
					String plName = rs1.next().get("fof.plName").asString();
					String plOwnerUsername = extractProfileUserName(plName);

					String query2 = "MATCH (nProfile: profile {userName: '"+ plOwnerUsername + "'})-[:includes*1]->(fof)" +
							" RETURN fof.songId";

					StatementResult rs2 = trans.run(query2);
					trans.success();
					List<String> songIds = new ArrayList<String>();

					while(rs2.hasNext())
						songIds.add(rs2.next().get("fof.songId").asString());

					data.put(plName, songIds);
				}

				qStatus = new DbQueryStatus("first stage of fetching friends' songs complete ", DbQueryExecResult.QUERY_OK);

			} catch (Exception e) {
				session.close();
				qStatus = new DbQueryStatus("Query didn't execute", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return qStatus;
			}
			session.close();
		}

		return qStatus;
	}

	protected Map<String, List<String>> getPlaylistSongIdList(){
		return data;
	}

	public String extractProfileUserName(String plName){
		String userName = new String();

		for(int i = 0; i < plName.length(); i++){
			if(plName.charAt(i) == '-') break;
			userName += plName.charAt(i);
		}

		return userName;
	}

	public DbQueryStatus subscribeToPlaylist(String userName, String plName) {
		DbQueryStatus qStatus = null;
		
		String query1 = "MATCH (nPlaylist: playlist)"
				+ " WHERE nPlaylist.plName='"+plName+"' RETURN nPlaylist.access";
		
		String query2  = "MATCH (nProfile: profile), (nPlaylist: playlist) " +
				"WHERE nProfile.userName = '" + userName + "' AND nPlaylist.plName = '" + plName + "' " +
				"CREATE (nProfile)-[:subscribes]->(nPlaylist)";
		
		String query3 = "MATCH (nProfile1: profile {userName: '" + userName + "' })" +
				"-[r:subscribes]->(nPlaylist: playlist {plName: '" + plName + "'})" +
				" RETURN nPlaylist.plName";

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {

				StatementResult rs = trans.run(query1);
				trans.success();
				
				if(rs.hasNext()) {
					String access = rs.next().get("nPlaylist.access").asString();
				
					if(access.equals("no"))
						qStatus = new DbQueryStatus("Profile: " + userName + " cannot subscibe to playlist: " + plName, DbQueryExecResult.QUERY_OK);
					else {

						StatementResult rs2 = trans.run(query3);
						trans.success();
						
						if(!rs2.hasNext()) {
							trans.run(query2);
							trans.success();
							qStatus = new DbQueryStatus("Profile: " + userName + " subcribed to playlist: " + plName, DbQueryExecResult.QUERY_OK);
						}
						else
							qStatus = new DbQueryStatus("Profile: " + userName + " already subcribed to playlist: " + plName, DbQueryExecResult.QUERY_OK);
					}
				}
				else
					qStatus = new DbQueryStatus("Playlist: " + plName + " doesn't exist", DbQueryExecResult.QUERY_ERROR_GENERIC);

				

			} catch (Exception e) {
				session.close();
				qStatus = new DbQueryStatus("Query didn't execute", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return qStatus;
			}
			session.close();
		}

		return qStatus;

	}
	
	public DbQueryStatus unSubscribeToPlaylist(String userName, String plName) {
		DbQueryStatus qStatus = null;
	
		String query  = "MATCH (nProfile: profile {userName: '"+userName+"'})"
				+ "-[r:subscribes]->(nPlaylist: playlist {plName:'"+plName+"', access: 'yes'}) DELETE r";
		
		String query2 = "MATCH (nProfile1: profile {userName: '" + userName + "' })" +
				"-[r:subscribes]->(nPlaylist: playlist {plName: '" + plName + "'})" +
				" RETURN nPlaylist.plName";
		

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult rs = trans.run(query2);
				trans.success();
				
				if(rs.hasNext()) {
					trans.run(query);
					trans.success();
					
					qStatus = new DbQueryStatus("Profile: " + userName + " unsubcribed to playlist: " + plName, DbQueryExecResult.QUERY_OK);
				}
				else
					qStatus = new DbQueryStatus("Profile: " + userName + " was never subscribed to playlist: " + plName, DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				
			} catch (Exception e) {
				session.close();
				qStatus = new DbQueryStatus("Query didn't execute", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return qStatus;
			}
			session.close();
		}

		return qStatus;

	}

	public DbQueryStatus changePlaylistAccess(String plName){
		DbQueryStatus qStatus;

		String query1 = "MATCH (nPlaylist: playlist)"
				+ " WHERE nPlaylist.plName='"+plName+"' RETURN nPlaylist.access";

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {

				StatementResult rs = trans.run(query1);
				trans.success();
				
				String access = rs.next().get("nPlaylist.access").asString();
	
				if(access.equals("no")) {
					
					String query2 = "MATCH (nPlaylist: playlist)"
							+ " WHERE nPlaylist.plName='"+plName+"'SET nPlaylist.access = 'yes'";
					trans.run(query2);
				}
				else{
					String query2 = "MATCH (nPlaylist: playlist)"
							+ " WHERE nPlaylist.plName='"+plName+"'SET nPlaylist.access = 'no'";

					String query3 = "MATCH (nProfile1: profile)" +
							"-[r:subscribes]->(nPlaylist: playlist {plName:'" + plName + "', access: 'no'}) " +
							"DELETE r";

					trans.run(query2);
					trans.run(query3);
				}

				trans.success();

				qStatus = new DbQueryStatus("Playlist: " + plName + " access was changed", DbQueryExecResult.QUERY_OK);

			} catch (Exception e) {
				session.close();
				qStatus = new DbQueryStatus("Query didn't execute", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return qStatus;
			}
			session.close();
		}

		return qStatus;
	}
}
