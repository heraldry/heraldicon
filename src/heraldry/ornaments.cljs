(ns heraldry.ornaments
  (:require [heraldry.interface :as interface]))

(def default-options
  {})

(defmethod interface/component-options :heraldry.component/ornaments [_path _data]
  default-options)
