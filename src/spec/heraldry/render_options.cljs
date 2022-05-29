(ns spec.heraldry.render-options
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.render.mode :as mode]
   [heraldicon.render.scope :as scope]
   [heraldicon.render.texture :as texture]
   [heraldicon.render.theme :as theme]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldry.render-options/type #{:heraldry/render-options})

(s/def :heraldry.render-options/escutcheon (s/or :none #{:none}
                                                 :escutcheon (su/key-in? escutcheon/escutcheon-map)))
(s/def :heraldry.render-options/mode (su/key-in? mode/mode-map))
(s/def :heraldry.render-options/theme (su/key-in? theme/theme-map))
(s/def :heraldry.render-options/texture (s/or :none #{:none}
                                              :texture (su/key-in? texture/texture-map)))
(s/def :heraldry.render-options/texture-displacement? boolean?)
(s/def :heraldry.render-options/shiny? boolean?)
(s/def :heraldry.render-options/escutcheon-shadow? boolean?)
(s/def :heraldry.render-options/escutcheon-outline? boolean?)
(s/def :heraldry.render-options/outline? boolean?)
(s/def :heraldry.render-options/squiggly? boolean?)
(s/def :heraldry.render-options/coat-of-arms-angle number?)
(s/def :heraldry.render-options/scope (su/key-in? scope/scope-map))

(s/def :heraldry/render-options (s/keys :req-un [:heraldry.render-options/type]
                                        :opt-un [:heraldry.render-options/escutcheon
                                                 :heraldry.render-options/mode
                                                 :heraldry.render-options/theme
                                                 :heraldry.render-options/texture
                                                 :heraldry.render-options/texture-displacement?
                                                 :heraldry.render-options/shiny?
                                                 :heraldry.render-options/escutcheon-shadow?
                                                 :heraldry.render-options/escutcheon-outline?
                                                 :heraldry.render-options/outline?
                                                 :heraldry.render-options/squiggly?
                                                 :heraldry.render-options/coat-of-arms-angle
                                                 :heraldry.render-options/scope]))
