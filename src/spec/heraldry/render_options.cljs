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

(s/def :heraldry.render-options/escutcheon (s/nilable (s/or :none #{:none}
                                                            :escutcheon (su/key-in? escutcheon/choice-map))))
(s/def :heraldry.render-options/mode (s/nilable (su/key-in? mode/mode-map)))
(s/def :heraldry.render-options/theme (s/nilable (su/key-in? theme/theme-map)))
(s/def :heraldry.render-options/texture (s/nilable (s/or :none #{:none}
                                                         :texture (su/key-in? texture/texture-map))))
(s/def :heraldry.render-options/texture-displacement? (s/nilable boolean?))
(s/def :heraldry.render-options/shiny? (s/nilable boolean?))
(s/def :heraldry.render-options/escutcheon-shadow? (s/nilable boolean?))
(s/def :heraldry.render-options/escutcheon-outline? (s/nilable boolean?))
(s/def :heraldry.render-options/outline? (s/nilable boolean?))
(s/def :heraldry.render-options/squiggly? (s/nilable boolean?))
(s/def :heraldry.render-options/coat-of-arms-angle (s/nilable number?))
(s/def :heraldry.render-options/scope (s/nilable (su/key-in? scope/scope-map)))

(s/def :heraldry/render-options (s/nilable (s/keys :req-un [:heraldry.render-options/type]
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
                                                            :heraldry.render-options/scope])))
