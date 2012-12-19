package com.balopat.codingboard

import akka.actor.ActorSystem
import _root_.akka.dispatch._
import org.scalatra._
import scalate.ScalateSupport
import scala.collection.immutable.List
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.json4s._
import JsonDSL._
import org.atmosphere.cpr.MetaBroadcaster
import org.scalatra.akka.AkkaSupport
import actors.{Actor,TIMEOUT}
import Actor._


class CodingBoardServlet(boards: CodingBoards = CodingBoards.instance) extends ScalatraServlet
  with ScalateSupport with JValueResult
  with JacksonJsonSupport with SessionSupport
  with AkkaSupport {  

    implicit protected val jsonFormats: Formats = DefaultFormats
    implicit val system = ActorSystem("CodingBoardActors")

    get("/") {
       index()
    }

    post("/submitboard") {
       val board = params("board")
       val lifeTime = params("lifeTimeInMinutes")
       try {
           contentType="text/html"
           if (boards.exists(board)) {
              jade("createboard", "boardErrorMessage" -> "A board with this name already exists!", "board" -> board)
           } else if (board == null || board.isEmpty) {
              jade("createboard", "boardErrorMessage" -> "Please name your board!", "board" -> board)
           } else {
               boards.create(board, lifeTime.toInt, System.currentTimeMillis)
               val evictionTime = lifeTime.toInt * 60000
               actor {
                  receiveWithin(evictionTime) {
                    case TIMEOUT => boards.remove(board)
                  }
                }
              joinCodingBoard(board)
           }
        } catch {
          case _ => jade("createboard", "lifeTimeErrorMessage" -> "How long will your session be?", "board" -> board, "lifeTime" -> lifeTime)
        }
    }

    get("/boards/:board") {
       joinCodingBoard(params("board"))
    }

    get("/boards/:board/post") {
       joinCodingBoard(params("board"))
    }

    post("/boards/:board/post") {
       val board = params("board")
       val formToken = params("formtoken")
       whenCodingBoardExistsOtherwiseErrorOnHomePage(
            board, 
            { 
               val codeSnippet = 
                  new CodeSnippet(formToken, 
                       params("description"), 
                       params("code"), 
                       params("language"), 
                       System.currentTimeMillis)
               boards.get(board) += codeSnippet
               joinCodingBoard(board )
            }
       )
    }

    post("/boards/:board/refresh") {
      val board = params("board")
       if (boardChanged(board, params("lastCodeSnippetId")))
       {
          compact(render(codeSnippets(board).last.toJSON))
       } else {
          compact(render("refresh"->"norefresh"))
       }
    }

    def boardChanged(board: String, lastCodeSnippetId: String) = {
       (!codeSnippets(board).isEmpty) && (!codeSnippets(board).last.id.equals(lastCodeSnippetId))
    }
  
    def codeSnippets(board: String) = boards.get(board).codeSnippets

    get("/boards/:board/codesnippet") {
       val board = params("board")
       contentType="text/html"
       whenCodingBoardExistsOtherwiseErrorOnHomePage(
            board, 
            jade("codesnippet", 
                   "formtoken" -> java.util.UUID.randomUUID.toString,  
                   "board" -> board)
       )
    }
       
    def index(extraAttributes: (String, Any)*) = {
       contentType="text/html"
       jade("index", ("boards" -> boards.list :: extraAttributes.toList).toArray: _*)
    }

    def joinCodingBoard(board: String, extraAttributes: (String, Any)*) = {
       contentType="text/html"
       whenCodingBoardExistsOtherwiseErrorOnHomePage(
          board, 
          jade("board", ("board" -> boards.get(board) :: extraAttributes.toList).toArray: _*  )
       )
    }
   
    def whenCodingBoardExistsOtherwiseErrorOnHomePage(board: String, boardExistsPage: =>Any) = {
       if (boards.exists(board)) {
          boardExistsPage
       } else {
          index("errorMessage" -> "CodingBoard not found!")
       }
    }

    
  notFound {
     // remove content type in case it was set through an action
     contentType = null
     // Try to render a ScalateTemplate if no route matched
     findTemplate(requestPath) map { path =>
        contentType = "text/html"
        layoutTemplate(path)
     } orElse serveStaticResource() getOrElse index()
  }
}