package db

import (
	"fmt"
	"os"
	"strings"
	"crypto/tls"
	"net"
	"gopkg.in/mgo.v2"
	"github.com/kataras/iris"
	"gopkg.in/mgo.v2/bson"
	"golang.org/x/crypto/bcrypt"
	"log"
	"github.com/sendgrid/sendgrid-go"
	"github.com/sendgrid/sendgrid-go/helpers/mail"
	"github.com/satori/go.uuid"
	"strconv"
	"github.com/NaySoftware/go-fcm"
	"time"
)
const (
	serverKey = "AAAA3UDdock:APA91bEVSzbr2ETJPBJe2DtWKPI4HEtaz6Yk1hCIve-0JG7MYbamOthjhMhQvPKNQ4qZ8K-zBdYzT2dwoIzpaS3TYzXME_ArNCvZKEqV8zuQb5oP6aFyKK8aBXki3R7iY5mqPQQTvQP8"
)

var Session *mgo.Session = nil
func Connect() *mgo.Session{
	uri := "mongodb://zbd1023:9ol.6yhn@cluster0-shard-00-00-2nmym.mongodb.net:27017,cluster0-shard-00-01-2nmym.mongodb.net:27017,cluster0-shard-00-02-2nmym.mongodb.net:27017/test?replicaSet=Cluster0-shard-0&authSource=admin"
	if uri == "" {
		fmt.Println("No connection string provided - set MONGODB_URL")
		os.Exit(1)
	}
	uri = strings.TrimSuffix(uri, "?ssl=true")

	tlsConfig := &tls.Config{}
	tlsConfig.InsecureSkipVerify = true

	dialInfo, err := mgo.ParseURL(uri)

	if err != nil {
		fmt.Println("Failed to parse URI: ", err)
		os.Exit(1)
	}

	dialInfo.DialServer = func(addr *mgo.ServerAddr) (net.Conn, error) {
		conn, err := tls.Dial("tcp", addr.String(), tlsConfig)
		return conn, err
	}

	session, err := mgo.DialWithInfo(dialInfo)
	if err != nil {
		fmt.Println("Failed to connect: ", err)
		os.Exit(1)
	}
	return session

}

type User struct {
	Name string
	Username string
	Password string
	Activated bool
	Activation string
}

type Response struct {
	Errorcode int `json:"error"`
}

type Responses struct {
	Errorcode int `json:"errorcode"`
	Username string `json:"username"`
}

type Location struct{
	Email string
	Latitude float64
	Longitude float64
}
type Pokes struct{
	Email string
	Pokes []string
}

type Response_location struct {
	Data []Location `json:"data"`
}

type Status struct{
	Email string
	Status string
	Last_updated_time time.Time
}

type Token struct{
	Email string
	Token string
}

type Response_status struct{
	Email string `json:"email"`
	Username string `json:"username"`
	Status string `json:"status"`
	Last_updated_time time.Time `json:"time"`
}

func Signup(ctx iris.Context){
	var name string = ctx.FormValue("name");
	var username string = ctx.FormValue("username")
	var password string = ctx.FormValue("password")

	authSession := Session.Copy()
	defer authSession.Close()
	result := User{}
	c := authSession.DB("app").C("auth")
	error := c.Find(bson.M{"username": username}).One(&result)
	if error != nil{
		if error.Error() == "not found"{
			bytePassword := []byte(password)
			hashedPassword,_ := bcrypt.GenerateFromPassword(bytePassword, bcrypt.DefaultCost)
			u2 := uuid.NewV4()
			c.Insert(&User{name, username, string(hashedPassword), false, u2.String()})
			res := Response{
				Errorcode:0,
			}
			ctx.JSON(res)
			email(username, u2.String())
			return
		}else{
			res := Response{
				Errorcode:2,
			}
			ctx.JSON(res)
			return
		}

	}else{
		res := Response{
			Errorcode:1,
		}
		ctx.JSON(res)
	}


}


func Login(ctx iris.Context){
	var username string = ctx.FormValue("username")
	var password string = ctx.FormValue("password")
	authSession := Session.Copy()
	defer authSession.Close()
	result := User{}
	c := authSession.DB("app").C("auth")
	error := c.Find(bson.M{"username": username}).One(&result)
	if error != nil{
		res := Responses{
			Errorcode:1,
			Username:"",
		}
		ctx.JSON(res)
	}else{
		err := bcrypt.CompareHashAndPassword([]byte(result.Password), []byte(password))
		if err != nil{
			res := Responses{
				Errorcode:2,
				Username:"",
			}
			ctx.JSON(res)
		}else{
			if !result.Activated{
				res := Responses{
					Errorcode:3,
					Username:"",
				}
				ctx.JSON(res)
			} else{
				res := Responses{
					Errorcode:0,
					Username:result.Name,
				}
				ctx.JSON(res)
			}
		}
	}
}

func Activate(ctx iris.Context){
	var username string = ctx.FormValue("username")
	var code string = ctx.FormValue("activation")
	authSession := Session.Copy()
	defer authSession.Close()
	c := authSession.DB("app").C("auth")
	query := bson.M{"username": username, "activation": code}
	change := bson.M{"$set": bson.M{"activated": true}}
	error := c.Update(query, change)
	if error != nil{
		ctx.Text("Something went wrong")
		return
	}
	ctx.HTML("Activation successful. You can now log in on your app.")
	// Go to MainActivity
}

func email(address string, activation string) {
	from := mail.NewEmail("Friender", "noreply@friender.com")
	subject := "Activate Your New Account"
	to := mail.NewEmail("User", address)
	m := mail.NewV3MailInit(from, subject, to)
	m.Personalizations[0].SetSubstitution("-name-", "Example User")
	m.Personalizations[0].SetSubstitution(
		"-link-",
		fmt.Sprintf("http://vcm-3269.vm.duke.edu/activate?username=%s&activation=%s", address, activation))
	m.SetTemplateID("846b8eb2-2016-41b2-990d-b5a7f629c247")
	request := sendgrid.GetRequest(os.Getenv("SENDGRID_API_KEY"), "/v3/mail/send", "https://api.sendgrid.com")
	request.Method = "POST"
	request.Body = mail.GetRequestBody(m)
	response, err := sendgrid.API(request)
	if err != nil {
		log.Println(err)
	} else {
		fmt.Println(response.StatusCode)
		fmt.Println(response.Body)
		fmt.Println(response.Headers)
	}
}

func Update_location(ctx iris.Context){
	email  := ctx.FormValue("email")
	latitude , _ := strconv.ParseFloat(ctx.FormValue("latitude"), 64)
	longitude, _ := strconv.ParseFloat(ctx.FormValue("longitude"), 64)
	session := Session.Copy()
	defer session.Close()
	c := session.DB("app").C("location")
	result := Location{}
	error := c.Find(bson.M{"email": email}).One(&result)
	if error != nil{
		c.Insert(&Location{email, latitude, longitude})
	}else{
		query := bson.M{"email": email}
		change := bson.M{"$set": bson.M{"latitude": latitude, "longitude": longitude}}
		c.Update(query, change)
	}
	ctx.Text("ok")
}

func Poke(ctx iris.Context){
	poker := ctx.FormValue("poker")
	pokee := ctx.FormValue("pokee")
	session := Session.Copy()
	defer session.Close()
	c := session.DB("app").C("pokes")
	result := Pokes{}
	error := c.Find(bson.M{"email": pokee}).One(&result)
	if error != nil{
		pokers := []string{poker}
		// poker poked pokee, so store poker in pokee's poked.
		c.Insert(&Pokes{pokee, pokers})
	}else{
		if !contains(result.Pokes, poker){
			p := append(result.Pokes, poker)
			query := bson.M{"email": pokee}
			change := bson.M{"$set": bson.M{"pokes": p}}
			c.Update(query, change)
		}
	}
	ctx.Text("ok")
}

func Grab_location(ctx iris.Context){
	session := Session.Copy()
	defer session.Close()
	c := session.DB("app").C("location")
	result := []Location{}
	c.Find(bson.M{}).All(&result)
	res := Response_location{
		Data: result,
	}
	ctx.JSON(res)
}

func Update_status(ctx iris.Context){
	email  := ctx.FormValue("email")
	status := ctx.FormValue("status")
	session := Session.Copy()
	defer session.Close()
	c := session.DB("app").C("status")
	result := Status{}
	error := c.Find(bson.M{"email": email}).One(&result)
	if error != nil{
		c.Insert(&Status{email, status, time.Now()})
	}else{
		query := bson.M{"email": email}
		change := bson.M{"$set": bson.M{"status": status, "last_updated_time": time.Now()}}
		c.Update(query, change)
	}
	ctx.Text("ok")
}

func Get_status(ctx iris.Context){
	email  := ctx.FormValue("email")
	session := Session.Copy()
	defer session.Close()
	c := session.DB("app").C("status")
	result := Status{}
	result1 := User{} 
	error := c.Find(bson.M{"email": email}).One(&result)
	if error != nil{
		ctx.Text("rip")
	}else{
		c := session.DB("app").C("auth")
		c.Find(bson.M{"username": email}).One(&result1)
		res := Response_status{
			Email:result.Email,
			Username:result1.Name,
			Status:result.Status,
			Last_updated_time:result.Last_updated_time,
		}
		ctx.JSON(res)
	}
}

func contains(arr []string, str string) bool {
	for _, a := range arr {
		if a == str {
			return true
		}
	}
	return false
}

func Update_token(ctx iris.Context){
	email  := ctx.FormValue("email")
	token := ctx.FormValue("token")
	session := Session.Copy()
	defer session.Close()
	c := session.DB("app").C("token")
	result := Token{}
	error := c.Find(bson.M{"email": email}).One(&result)
	if error != nil{
		c.Insert(&Token{email, token})
	}else{
		query := bson.M{"email": email}
		change := bson.M{"$set": bson.M{"token": token}}
		c.Update(query, change)
	}
	ctx.Text("ok")
}

func Send_poke(ctx iris.Context){
	email0  := ctx.FormValue("email0")

	email1  := ctx.FormValue("email1")
	session := Session.Copy()
	defer session.Close()
	c := session.DB("app").C("token")
	result1 := Token{}
	c.Find(bson.M{"email": email1}).One(&result1)
	result2 := User{}
	c = session.DB("app").C("auth")
	c.Find(bson.M{"username": email0}).One(&result2)
	fmt.Println(result1.Token)
	fmt.Println(result2.Name)
	data := map[string]string{
		"msg": "Hey you're poked by "+result2.Name,
		"body": "Hey you're poked by "+result2.Name,
	}
	clicent := fcm.NewFcmClient(serverKey)
	//token := "fTdJNvbOHR0:APA91bG0BWbTYgMJdQDhqW7aGAuwRKWeaCjQepoyWYGRFPUcyHTqOl-WC-0I4FtbNbs7ELp2wJ_2B5ur6XdxtKkx6NRQsg2S85-nK35kjPO8miM53JBHejsSKkxoo406a-_mgIhY55ow"
	clicent.NewFcmMsgTo(result1.Token, data)
	a := fcm.NotificationPayload{Body:"Hey you're poked by "+result2.Name}
	clicent.SetNotificationPayload(&a);
	status, err := clicent.Send()
	if err == nil {
		status.PrintResults()
	} else {
		fmt.Println(err)
	}
	//ctx.Text("ok")

}

