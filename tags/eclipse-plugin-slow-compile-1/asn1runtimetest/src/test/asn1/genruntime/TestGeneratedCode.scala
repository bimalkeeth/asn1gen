package ModuleName {
  import org.asn1gen.runtime._

  case class MySequence(
    field1: AsnInteger,
    field2: AsnInteger
  ) extends AsnSequence {
    def field1(f: (AsnInteger => AsnInteger)): MySequence = MySequence(
      f(this.field1),
      this.field2)
    def field2(f: (AsnInteger => AsnInteger)): MySequence = MySequence(
      this.field1,
      f(this.field2))
  }
}
package ModuleName {
  import org.asn1gen.runtime._

  case class MyChoice(override val choice: AsnType) extends AsnChoice(choice) {
    //////////////////////////////////////////////////////////////////
    // Choice IDs

    val CHOICE1 = 0: Integer

    val CHOICE2 = 1: Integer

    def choice1: AsnInteger = choice_.asInstanceOf[AsnInteger]

    def choice2: AsnInteger = choice_.asInstanceOf[AsnInteger]
  }
}