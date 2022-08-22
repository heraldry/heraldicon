(ns heraldicon.shared
  (:require
   ["paper" :refer [paper Size]]
   [heraldicon.entity.charge.data]
   [heraldicon.entity.collection.data]
   [heraldicon.entity.collection.element]
   [heraldicon.entity.core]
   [heraldicon.heraldry.charge-group.core]
   [heraldicon.heraldry.charge-group.options]
   [heraldicon.heraldry.charge.core]
   [heraldicon.heraldry.charge.options]
   [heraldicon.heraldry.charge.other]
   [heraldicon.heraldry.charge.render]
   [heraldicon.heraldry.coat-of-arms]
   [heraldicon.heraldry.cottising]
   [heraldicon.heraldry.field.core]
   [heraldicon.heraldry.motto]
   [heraldicon.heraldry.option.attributes]
   [heraldicon.heraldry.ordinary.core]
   [heraldicon.heraldry.ordinary.options]
   [heraldicon.heraldry.ordinary.render]
   [heraldicon.heraldry.ribbon]
   [heraldicon.heraldry.semy]
   [heraldicon.heraldry.tincture]
   [heraldicon.render.options]))

(.setup paper (new Size 500 500))
