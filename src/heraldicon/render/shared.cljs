(ns heraldicon.render.shared
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.interface :as interface]
   [heraldicon.render.options :as render.options]))

(defmethod interface/options :heraldry/render-options [context]
  (render.options/build {:mode (interface/get-raw-data (c/++ context :mode))
                         :texture (interface/get-raw-data (c/++ context :texture))
                         :escutcheon (interface/get-raw-data (c/++ context :escutcheon))
                         :with-root-escutcheon? (= (:path context)
                                                   (conj (form/data-path :heraldicon.entity.type/collection)
                                                         :data :render-options))}))
