package com.eecs3311.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.neo4j.driver.v1.Transaction;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.json.*;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;
	HashMap<String, List<String>> data;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			} catch (Exception e) {
				if (e.getMessage().contains("An equivalent constraint already exists")) {
					System.out.println("INFO: Profile constraints already exist (DB likely already initialized), should be OK to continue");
				} else {
					// something else, yuck, bye
					throw e;
				}
			}
			session.close();
		}
	}
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		DbQueryStatus qStatus;

		String queryStr1 = "CREATE (nProfile: profile {userName: '"+userName
				+"', fullName: '"+fullName+"', password: '"+password+"'})";

		String queryStr2 = "CREATE (nPlaylist: playlist {plName: '"+userName+"-favorites', access: 'no'})";

		String queryStr3 = "MATCH (nProfile: profile), (nPlaylist:playlist) " +
							"WHERE nProfile.userName = '"+userName+"' AND nPlaylist.plName = '"+userName+"-favorites' "
							+ "CREATE (nProfile)-[:created]->(nPlaylist)";

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {

				trans.run(queryStr1);
				trans.run(queryStr2);
				trans.run(queryStr3);

				trans.success();
				qStatus = new DbQueryStatus("Profile and playlists were created", DbQueryExecResult.QUERY_OK);

			} catch (Exception e) {
				session.close();
				qStatus = new DbQueryStatus("Error in query syntax or Profile already exist", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return qStatus;
			}
			session.close();
		}

		return qStatus;
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		DbQueryStatus qStatus;

		String queryStr = "MATCH (nProfile1: profile), (nProfile2: profile) " +
				"WHERE nProfile1.userName = '" + userName +
				"' AND nProfile2.userName = '" + frndUserName + "' "
				+ "CREATE (nProfile1)-[:follows]->(nProfile2)";
		
		String query2 = "MATCH (nProfile1: profile {userName: '" + userName + "' })" +
				"-[r:follows]->(nProfile2: profile {userName: '" + frndUserName + "'})" +
				" RETURN nProfile2.userName";

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult rs = trans.run(query2);
				trans.success();
				
				if(!rs.hasNext()) {
					trans.run(queryStr);
					trans.success();
					qStatus = new DbQueryStatus(userName + " follows " + frndUserName, DbQueryExecResult.QUERY_OK);
				}
				else
					qStatus = new DbQueryStatus(userName + " already follows " + frndUserName, DbQueryExecResult.QUERY_OK);

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
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		DbQueryStatus qStatus;

		String queryStr = "MATCH (nProfile1: profile {userName: '" + userName + "' })" +
				"-[r:follows]->(nProfile2: profile {userName: '" + frndUserName + "'})" +
				" DELETE r";
		

		String query2 = "MATCH (nProfile1: profile {userName: '" + userName + "' })" +
				"-[r:follows]->(nProfile2: profile {userName: '" + frndUserName + "'})" +
				" RETURN nProfile2.userName";

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult rs = trans.run(query2);
				trans.success();
				
				if(rs.hasNext()) {
					trans.run(queryStr);
					trans.success();
					qStatus = new DbQueryStatus(userName + " unfollows " + frndUserName, DbQueryExecResult.QUERY_OK);
				}
				else
					qStatus = new DbQueryStatus(userName + " does not follow " + frndUserName, DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

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
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		DbQueryStatus qStatus;

		String queryStr = "MATCH (nProfile: profile {userName: '" + userName + "'})-[:follows*1]->(fof)"
			+ " RETURN fof.userName";

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				data = new HashMap<String, List<String>>();

				StatementResult rs1 = trans.run(queryStr);
				trans.success();

				while(rs1.hasNext()) {
					String frndUserName = rs1.next().get("fof.userName").asString();
					
					String query = "MATCH (nProfile: profile {userName: '"+ frndUserName + "'})-[:includes*1]->(fof) RETURN fof.songId";

					StatementResult rs2 = trans.run(query);
					trans.success();
					List<String> songIds = new ArrayList<String>();

					while(rs2.hasNext())
						songIds.add(rs2.next().get("fof.songId").asString());
					
					data.put(frndUserName, songIds);
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

	public DbQueryStatus addSongId(String songId){
		DbQueryStatus qStatus;

		String query = "CREATE (s:song {songId: '"+songId+"'})";

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {

				trans.run(query);
				trans.success();
				qStatus = new DbQueryStatus("Song with songId: " + songId + " added to neo4j", DbQueryExecResult.QUERY_OK);

			} catch (Exception e) {
				session.close();
				qStatus = new DbQueryStatus("Query didn't execute", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return qStatus;
			}
			session.close();
		}

		return qStatus;
	}

	public DbQueryStatus deleteSongById(String songId){
		DbQueryStatus qStatus;

		String query1 = "MATCH (nProfile1: profile)" +
		"-[r:includes]->(nSong: song {songId:'" + songId + "'}) " +
		"DELETE r";

		String query2 = "MATCH (nSong: song {songId: '" + songId + "'}) DELETE nSong";

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {

				trans.run(query1);
				trans.run(query2);
				trans.success();
				qStatus = new DbQueryStatus("Song with songId: " + songId + " deleted from neo4j", DbQueryExecResult.QUERY_OK);

			} catch (Exception e) {
				session.close();
				qStatus = new DbQueryStatus("Query didn't execute", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return qStatus;
			}
			session.close();
		}

		return qStatus;
	}

	protected Map<String, List<String>> getFriendSongIdList(){
		return data;
	}
	
}
