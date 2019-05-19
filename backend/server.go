package main

import (
	"github.com/kataras/iris"
	"friender/backend/db"
)


func main() {
	db.Session = db.Connect()
	defer db.Session.Close()
	app := iris.Default()
	app.Post("/signup", db.Signup)
	app.Post("/login", db.Login)
	app.Post("/poke", db.Poke)
	app.Post("/update_location", db.Update_location)
	app.Post("/update_status", db.Update_status)
	app.Post("/get_status", db.Get_status)
	app.Post("/grab_location", db.Grab_location)
	app.Post("/update_token", db.Update_token)
	app.Post("/send_poke", db.Send_poke)
	app.Get("/activate", db.Activate)

	app.Run(iris.Addr(":8080"))
}
