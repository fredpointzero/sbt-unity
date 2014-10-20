using System;
using System.Linq;
using System.IO;

namespace FV
{
    public static class TouchFile
    {
        public static void Touch()
        {
            var commandLine = Environment.CommandLine;
            var tokens = commandLine.Split(' ');
            var files = tokens.SkipWhile(s => s != "-executeMethod").Skip(2);
            foreach (string path in files)
            {
                using(var stream = File.Create(path))
                {

                }
            }
        }
    }
}