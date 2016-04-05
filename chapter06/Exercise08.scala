trait RNG {
  def nextInt: (Int, RNG)
}

case class SimpleRNG(seed: Long) extends RNG {
  def nextInt: (Int, RNG) = {
    val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL
    val nextRNG = SimpleRNG(newSeed)
    val n = (newSeed >>> 16).toInt
    (n, nextRNG)
  }
}

type Rand[+A] = RNG => (A, RNG)

object SimpleRNG {
  def nonNegativeInt(rng: RNG): (Int, RNG) = {
    val (n, nextRNG) = rng.nextInt
    (if (n == Int.MinValue) 0 else Math.abs(n), nextRNG)
  }

  def double(rng: RNG): (Double, RNG) = {
    map(nonNegativeInt)(x => x / (Int.MaxValue.toDouble + 1))(rng)
  }

  def intDouble(rng: RNG): ((Int, Double), RNG) = {
    val (n, nextRNG) = rng.nextInt
    val (d, nextNextRNG) = double(nextRNG)
    ((n, d), nextNextRNG)
  }

  def doubleInt(rng: RNG): ((Double, Int), RNG) = {
    val (n, nextRNG) = rng.nextInt
    val (d, nextNextRNG) = double(nextRNG)
    ((d, n), nextNextRNG)
  }

  def double3(rng: RNG): ((Double, Double, Double), RNG) = {
    val (d1, nextRNG) = double(rng)
    val (d2, nextNextRNG) = double(nextRNG)
    val (d3, nextNextNextRNG) = double(nextNextRNG)
    ((d1, d2, d3), nextNextNextRNG)
  }

  def ints(count: Int)(rng: RNG): (List[Int], RNG) = sequence(List.fill(count)(int))(rng)

  val int: Rand[Int] = _.nextInt

  def unit[A](a: A): Rand[A] = {
    rng => (a, rng)
  }

  def map[A, B](s: Rand[A])(f: A => B): Rand[B] =
    rng => {
      val (a, rng2) = s(rng)
      (f(a), rng2)
    }

  def map2[A, B, C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] = {
    rng => {
      val (a, rng2) = ra(rng)
      val (b, rng3) = rb(rng2)
      (f(a, b), rng3)
    }
  }

  def sequence[A](fs: List[Rand[A]]): Rand[List[A]] = {
    rng => fs match {
      case h::t => {
        val (a, rng2) = h(rng)
        val (as, rng3) = sequence(t)(rng2)
        (a::as, rng3)
      }
      case _ => (Nil, rng)
    }
  }

  def flatMap[A, B](f: Rand[A])(g: A => Rand[B]): Rand[B] = {
    rng => {
      val (a, rng2) = f(rng)
      g(a)(rng2)
    }
  }
}


import SimpleRNG._

val rng = SimpleRNG(42)

val (is, r) = ints(10)(rng)
println(is)

