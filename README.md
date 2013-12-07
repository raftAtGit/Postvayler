Postvayler
==========

### Transparent Persistence for POJO's (Plain Old Java Objects)

*This library is still under development. At the moment, it's at the 'Proof of Concept' state. All contributions and feedback are welcome. Have a look at [todo list](Postvayler/doc/todo.txt) for tasks waiting.*

---
### Introduction

Postvayler provides persistance capabilities to [POJO](http://en.wikipedia.org/wiki/Plain_Old_Java_Object)'s. It requires **neither** implementing special interfaces **nor** extending from special classes **nor** a backing relational database. Only some *@Annotations* and conforming a few rules is necessary.

Here is a quick sample:
```
@Persistent
class Library {
   final Map<Integer, Book> books = new HashMap<Integer, Book>();
   int lastBookId = 1;
   
   @Persist
   void addBook(Book book) {
      book.setId(lastBookId++);
      books.put(book.getId(), book);
   }
}
```
Quite a Plain Old Java Object, isn't it? Run the **Postvayler compiler** after **javac** and then to get a reference to a persisted instance: 

```
Library library = Postvayler.create(Library.class);
```
or 
```
Library library = Postvayler.create(new Library());
```
Now, add as many books as you want to your library, kill your program and when you restart it the previously added books will be in your library.

### How it works

Postvayler uses [Prevayler](http://prevayler.org/) for persistance. Prevayler is a brilliant library to persist POJO's. In short it says: 

> Encapsulate all changes to your data into *Transaction* classes and pass over me. I will write those transactions to disk and then execute on your data. When the program is restarted, I will execute those transactions in the same order on your data, provided all such changes are [deterministic](http://en.wikipedia.org/wiki/Deterministic_system), we will end up with the exact same state just before the program terminated last time.

This is simply a brilliant idea to persist POJO's. But the thing is writing *Transaction* classes for each change is simply too verbose and [boilerplate code](http://en.wikipedia.org/wiki/Boilerplate_code). And there is also the famous [Baptism problem](http://prevayler.codehaus.org/The+Baptism+Problem) to watch over.

Here Postvayler comes into scene. It injects bytecode into (instruments) **javac** compiled *@Persistent* classes such that every *@Persist* method in a *@Persistent* class is modified to execute that method via Prevayler.

For example, the *addBook(Book)* method in the previous sample becomes something like:
```
void addBook(Book book) {
  if (! there is Postvayler context) {
     // no persistence, just proceed to original method
     __postvayler_addBook(book);
     return;
  }
  // we are already encapsulated in a transaction, just proceed to original method
  if (weAreInATransaction) {
     __postvayler_addBook(book);
     return;
  }
  weAreInATransaction = true;
  try {
    prevayler.execute(new aTransactionDescribingThisMethodCall());
  } finally {
    weAreInATransaction = false;
  }
}
// original addBook method is renamed to this
private void __postvayler_addBook(Book book) {
  // the contents of the original addBook method
}
```
As can been seen, if there is no *Postvayler context* around, the object bahaves like the original POJO with an ignorable overhead.

Constructors of *@Persistent* classes are also instrumented to keep track of of them.

Well, that's it in a glance :) If interested, have a look at the [Bank sample](Postvayler/samples/raft/postvayler/samples/bank) and the [emulated Bank sample](Postvayler/samples/raft/postvayler/samples/_bank) where the injected bytecode is manually added to demonstrate what is going on.

Cheers and happy persisting,

*r a f t*

