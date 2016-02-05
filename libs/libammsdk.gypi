{
  'variables' : {
      'prebuiltjar': '<!(python <(DEPTH)/build/dir_exists.py ../../../swe/smartisan/libs/)',
      #'debug': "<!(echo <(prebuiltjar) 1>&2)",
  },
  'targets' : [
    {
      'target_name': 'libammsdk',
      'type': 'none',
      'conditions': [
        ['prebuiltjar == "True"', {
          'variables': {
            'jar_path': './libammsdk.jar',
          },
          'includes':['../../../build/java_prebuilt.gypi']
        }],
        ['srcbuild == "False"', {
        }],
      ],
    },
  ],
}
