(ns heraldry.coat-of-arms.ordinary.core
  (:require [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
            [heraldry.frontend.util :as frontend-util]
            [heraldry.interface :as interface]))

(defmethod interface/render-component :heraldry.component/ordinary [path parent-path environment context]
  (ordinary-interface/render-ordinary path parent-path environment context))

(defn title [ordinary]
  (-> ordinary :type frontend-util/translate-cap-first))
