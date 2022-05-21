(ns spec.heraldry.motto.geometry
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.motto.geometry/size number?)

(s/def :heraldry.motto/geometry (s/keys :opt-un [:heraldry.motto.geometry/size]))
