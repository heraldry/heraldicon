(ns heraldry.ornaments
  (:require
   [heraldry.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/ornaments [_context]
  #{})

(defmethod interface/options :heraldry.component/ornaments [_context]
  {})
