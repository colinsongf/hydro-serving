package io.hydrosphere.serving.manager.domain.host_selector

import cats.Monad
import cats.data.EitherT
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.hydrosphere.serving.manager.domain.DomainError

trait HostSelectorService[F[_]] {
  def create(name: String, placeholder: String): F[Either[DomainError, HostSelector]]

  def delete(id: Long): F[Either[DomainError, HostSelector]]
}

object HostSelectorService {
  def apply[F[_] : Monad](hsRepo: HostSelectorRepository[F]): HostSelectorService[F] = new HostSelectorService[F] {

    def create(name: String, placeholder: String): F[Either[DomainError, HostSelector]] = {
      hsRepo.get(name).flatMap {
        case Some(_) => Monad[F].pure(Left(DomainError.invalidRequest(s"HostSelector $name already exists")))
        case None =>
          val environment = HostSelector(
            name = name,
            placeholder = placeholder,
            id = 0L
          )
          hsRepo.create(environment).map(Right(_))
      }
    }

    def delete(id: Long): F[Either[DomainError, HostSelector]] = {
      val f = for {
        hs <- EitherT.fromOptionF[F, DomainError, HostSelector](hsRepo.get(id), DomainError.notFound(s"Can't find HostSelector with id $id"))
        _ <- EitherT.liftF[F, DomainError, Int](hsRepo.delete(hs.id))
      } yield hs
      f.value
    }
  }
}