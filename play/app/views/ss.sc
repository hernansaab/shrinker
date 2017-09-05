def log(message: String , level: String="avc" ) = println(s">>>>$level: $message")

log(message = "sds")




def foo(a: String)(b: Int = 42) = a + b
def foo(a: Int)   (b: Int = 42) = a + b

println ("xxxx"+foo(2)(3))

foo("ss")(3)


